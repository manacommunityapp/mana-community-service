package com.manacommunity.api.devices.service;

import com.manacommunity.api.devices.adapter.SmartDeviceApiAdapter;
import com.manacommunity.api.devices.dto.SmartDeviceDtos.*;
import com.manacommunity.api.devices.entity.DeviceControlLog;
import com.manacommunity.api.devices.entity.DeviceVisitorOtp;
import com.manacommunity.api.devices.entity.SmartDevice;
import com.manacommunity.api.devices.repository.DeviceControlLogRepository;
import com.manacommunity.api.devices.repository.DeviceVisitorOtpRepository;
import com.manacommunity.api.devices.repository.SmartDeviceRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.platform.audit.AuditJournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartDeviceService {

    private static final SecureRandom RNG = new SecureRandom();

    private final SmartDeviceRepository deviceRepo;
    private final DeviceControlLogRepository logRepo;
    private final DeviceVisitorOtpRepository otpRepo;
    private final SmartDeviceApiAdapter vendorAdapter;
    private final AuditJournalService auditJournalService;

    // ── Registry ──────────────────────────────────────────────────────────

    @Transactional
    public DeviceResponse registerDevice(RegisterDeviceRequest req) {
        SmartDevice device = SmartDevice.builder()
                .communityId(req.communityId())
                .deviceName(req.deviceName())
                .deviceType(req.deviceType())
                .location(req.location())
                .vendor(req.vendor())
                .model(req.model())
                .apiEndpoint(req.apiEndpoint())
                .apiKeyRef(req.apiKeyRef())
                .firmwareVersion(req.firmwareVersion())
                .accessibleBy(req.accessibleBy() != null ? req.accessibleBy() : "FM_ONLY")
                .build();
        return toResponse(deviceRepo.save(device));
    }

    @Transactional
    public DeviceResponse updateDevice(Long deviceId, RegisterDeviceRequest req) {
        SmartDevice device = findDevice(deviceId);
        device.setDeviceName(req.deviceName());
        device.setDeviceType(req.deviceType());
        device.setLocation(req.location());
        device.setVendor(req.vendor());
        device.setModel(req.model());
        device.setApiEndpoint(req.apiEndpoint());
        device.setApiKeyRef(req.apiKeyRef());
        device.setFirmwareVersion(req.firmwareVersion());
        if (req.accessibleBy() != null) {
            device.setAccessibleBy(req.accessibleBy());
        }
        return toResponse(deviceRepo.save(device));
    }

    public List<DeviceResponse> getDevices(Long communityId, String type) {
        List<SmartDevice> devices = (type != null && !type.isBlank())
                ? deviceRepo.findByCommunityIdAndDeviceType(communityId, type)
                : deviceRepo.findByCommunityId(communityId);
        return devices.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DeviceResponse getDevice(Long deviceId) {
        return toResponse(findDevice(deviceId));
    }

    // ── Control ───────────────────────────────────────────────────────────

    @Transactional
    public DeviceControlResult controlDevice(Long deviceId, String action,
                                              Long actorId, String actorRole,
                                              String requestSource) {
        SmartDevice device = findDevice(deviceId);
        assertPermission(device, actorRole);

        DeviceControlLog entry = DeviceControlLog.builder()
                .deviceId(deviceId)
                .communityId(device.getCommunityId())
                .action(action)
                .actorId(actorId)
                .actorRole(actorRole)
                .requestSource(requestSource)
                .result("PENDING")
                .build();
        entry = logRepo.save(entry);

        auditJournalService.recordSecurity(
                "DEVICE_CONTROL_ATTEMPT",
                actorId,
                null,
                new java.util.HashMap<>(java.util.Map.of(
                        "deviceId", deviceId, "action", action, "actorRole", actorRole)));

        SmartDeviceApiAdapter.AdapterResult adapterResult;
        try {
            adapterResult = vendorAdapter.execute(device, action);
        } catch (Exception ex) {
            log.error("Vendor adapter error deviceId={} action={}", deviceId, action, ex);
            adapterResult = new SmartDeviceApiAdapter.AdapterResult("FAILED", ex.getMessage(), 0);
        }

        entry.setResult(adapterResult.result());
        entry.setErrorMessage(adapterResult.errorMessage());
        entry.setDurationMs(adapterResult.durationMs());
        logRepo.save(entry);

        return new DeviceControlResult(entry.getId(), adapterResult.result(),
                adapterResult.errorMessage(), adapterResult.durationMs());
    }

    // ── Visitor OTP ───────────────────────────────────────────────────────

    @Transactional
    public OtpResponse issueVisitorOtp(Long deviceId, Long residentId,
                                        String visitorName, int validMinutes) {
        SmartDevice device = findDevice(deviceId);
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        Instant now = Instant.now();
        DeviceVisitorOtp otp = DeviceVisitorOtp.builder()
                .deviceId(deviceId)
                .communityId(device.getCommunityId())
                .issuedBy(residentId)
                .otpCode(code)
                .visitorName(visitorName)
                .validFrom(now)
                .validUntil(now.plusSeconds((long) validMinutes * 60))
                .build();
        otp = otpRepo.save(otp);
        return new OtpResponse(otp.getId(), otp.getOtpCode(), otp.getVisitorName(),
                otp.getValidFrom(), otp.getValidUntil());
    }

    @Transactional
    public boolean verifyAndUseOtp(Long deviceId, String otpCode) {
        return otpRepo.findValid(deviceId, otpCode, Instant.now())
                .map(otp -> {
                    otp.setUsedAt(Instant.now());
                    otpRepo.save(otp);
                    log.info("Visitor OTP used: deviceId={} visitor={}", deviceId, otp.getVisitorName());
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void revokeOtp(Long otpId, Long residentId) {
        DeviceVisitorOtp otp = otpRepo.findById(otpId)
                .orElseThrow(() -> new ResourceNotFoundException("OTP not found"));
        if (!otp.getIssuedBy().equals(residentId)) {
            throw new SecurityException("Cannot revoke OTP issued by another resident");
        }
        otp.setRevoked(true);
        otpRepo.save(otp);
    }

    // ── Logs ──────────────────────────────────────────────────────────────

    public Page<DeviceControlLogResponse> getControlLog(Long deviceId, Pageable pageable) {
        return logRepo.findByDeviceIdOrderByOccurredAtDesc(deviceId, pageable)
                .map(this::toLogResponse);
    }

    public Page<DeviceControlLogResponse> getMyActions(Long actorId, Pageable pageable) {
        return logRepo.findByActorIdOrderByOccurredAtDesc(actorId, pageable)
                .map(this::toLogResponse);
    }

    @Transactional
    public void updateHeartbeat(Long deviceId) {
        SmartDevice device = findDevice(deviceId);
        device.setLastHeartbeat(Instant.now());
        device.setOnline(true);
        deviceRepo.save(device);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private SmartDevice findDevice(Long deviceId) {
        return deviceRepo.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));
    }

    private void assertPermission(SmartDevice device, String actorRole) {
        String level = device.getAccessibleBy();
        boolean permitted = switch (level) {
            case "FM_ONLY" -> "FM".equals(actorRole) || "ADMIN".equals(actorRole);
            case "SECURITY_STAFF" -> "SECURITY".equals(actorRole) || "FM".equals(actorRole) || "ADMIN".equals(actorRole);
            case "ALL_RESIDENTS" -> true;
            case "RESIDENT_OWN" -> "RESIDENT".equals(actorRole) || "FM".equals(actorRole) || "ADMIN".equals(actorRole);
            default -> "ADMIN".equals(actorRole);
        };
        if (!permitted) {
            throw new SecurityException("Actor role " + actorRole +
                    " cannot control device with accessibleBy=" + level);
        }
    }

    private DeviceResponse toResponse(SmartDevice d) {
        return new DeviceResponse(d.getId(), d.getCommunityId(), d.getDeviceName(),
                d.getDeviceType(), d.getLocation(), d.getVendor(), d.getModel(),
                d.getFirmwareVersion(), d.isOnline(), d.getLastHeartbeat(),
                d.getAccessibleBy(), d.getCreatedAt());
    }

    private DeviceControlLogResponse toLogResponse(DeviceControlLog l) {
        return new DeviceControlLogResponse(l.getId(), l.getDeviceId(), l.getAction(),
                l.getActorId(), l.getActorRole(), l.getRequestSource(), l.getResult(),
                l.getErrorMessage(), l.getDurationMs(), l.getOccurredAt());
    }
}
