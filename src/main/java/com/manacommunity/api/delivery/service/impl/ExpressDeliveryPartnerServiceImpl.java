package com.manacommunity.api.delivery.service.impl;

import com.manacommunity.api.delivery.dto.*;
import com.manacommunity.api.delivery.model.*;
import com.manacommunity.api.delivery.repository.ExpressDeliveryPassRepository;
import com.manacommunity.api.delivery.repository.ResidentDeliveryPreferenceRepository;
import com.manacommunity.api.delivery.service.ExpressDeliveryPartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpressDeliveryPartnerServiceImpl implements ExpressDeliveryPartnerService {

    private final ExpressDeliveryPassRepository passRepository;
    private final ResidentDeliveryPreferenceRepository preferenceRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public ExpressEntryResponse registerDispatch(ExpressEntryDispatchRequest req, String partnerKey) {
        log.info("Express Entry dispatch ingest from partner={} orderId={} flat={} communityId={}",
                req.getPartner(), req.getOrderId(), req.getFlatNumber(), req.getCommunityId());

        // Check resident delivery preferences
        ResidentDeliveryPreference pref = preferenceRepository
                .findByCommunityIdAndFlatNumber(req.getCommunityId(), req.getFlatNumber())
                .orElse(null);

        boolean autoApproved = isPartnerAutoApproved(pref, req.getPartner());
        boolean leaveAtGate = pref != null && Boolean.TRUE.equals(pref.getDefaultLeaveAtGate());
        String gateInstructions = (pref != null && pref.getDeliveryInstructions() != null)
                ? pref.getDeliveryInstructions()
                : "Proceed directly to " + (req.getTower() != null ? req.getTower() + ", " : "") + "Flat " + req.getFlatNumber();

        // Check if existing pass exists for this partner order
        ExpressDeliveryPass pass = passRepository
                .findByPartnerAndOrderId(req.getPartner(), req.getOrderId())
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        int validityMins = (req.getEtaMinutes() != null && req.getEtaMinutes() > 0) ? req.getEtaMinutes() + 60 : 90;

        if (pass == null) {
            String passCode = generatePassCode();
            String barcodePayload = "EXP:" + req.getCommunityId() + ":" + req.getPartner().name() + ":" + req.getOrderId() + ":" + passCode;

            pass = ExpressDeliveryPass.builder()
                    .orderId(req.getOrderId())
                    .partner(req.getPartner())
                    .communityId(req.getCommunityId())
                    .flatNumber(req.getFlatNumber())
                    .residentName(req.getResidentName())
                    .residentPhone(req.getResidentPhone())
                    .riderName(req.getRiderName())
                    .riderPhone(req.getRiderPhone())
                    .vehicleNumber(req.getVehicleNumber())
                    .riderPhotoUrl(req.getRiderPhotoUrl())
                    .itemsSummary(req.getItemsSummary())
                    .entryPassCode(passCode)
                    .barcodePayload(barcodePayload)
                    .status(autoApproved ? ExpressPassStatus.PRE_APPROVED : ExpressPassStatus.PRE_APPROVED)
                    .leaveAtGate(leaveAtGate)
                    .gateInstructions(gateInstructions)
                    .validFrom(now)
                    .validUntil(now.plusMinutes(validityMins))
                    .build();
        } else {
            // Update rider or ETA details
            pass.setRiderName(req.getRiderName());
            if (req.getRiderPhone() != null) pass.setRiderPhone(req.getRiderPhone());
            if (req.getVehicleNumber() != null) pass.setVehicleNumber(req.getVehicleNumber());
            pass.setValidUntil(now.plusMinutes(validityMins));
        }

        pass = passRepository.save(pass);

        return ExpressEntryResponse.builder()
                .success(true)
                .id(pass.getId())
                .orderId(pass.getOrderId())
                .partner(pass.getPartner())
                .entryPassCode(pass.getEntryPassCode())
                .barcodePayload(pass.getBarcodePayload())
                .status(pass.getStatus())
                .flatNumber(pass.getFlatNumber())
                .riderName(pass.getRiderName())
                .vehicleNumber(pass.getVehicleNumber())
                .leaveAtGate(pass.getLeaveAtGate())
                .gateInstructions(pass.getGateInstructions())
                .message("Express Gate Pass successfully issued and pre-approved.")
                .validFrom(pass.getValidFrom())
                .validUntil(pass.getValidUntil())
                .build();
    }

    @Override
    @Transactional
    public ExpressVerifyResponse verifyEntry(ExpressVerifyRequest req) {
        String code = req.getEntryCode() != null ? req.getEntryCode().trim() : "";
        log.info("Express Entry verification attempt code={} gate={}", code, req.getGateId());

        ExpressDeliveryPass pass = passRepository.findByEntryPassCode(code)
                .or(() -> passRepository.findByBarcodePayload(code))
                .or(() -> {
                    // Try with EXP- prefix if user typed only digits
                    if (!code.startsWith("EXP-") && code.matches("\\d+")) {
                        return passRepository.findByEntryPassCode("EXP-" + code);
                    }
                    return java.util.Optional.empty();
                })
                .orElse(null);

        if (pass == null) {
            return ExpressVerifyResponse.builder()
                    .allowed(false)
                    .message("Express delivery pass not found for code: " + code)
                    .barrierAction("HOLD")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(pass.getValidUntil())) {
            pass.setStatus(ExpressPassStatus.EXPIRED);
            passRepository.save(pass);
            return ExpressVerifyResponse.builder()
                    .allowed(false)
                    .passId(pass.getId())
                    .orderId(pass.getOrderId())
                    .partner(pass.getPartner())
                    .status(ExpressPassStatus.EXPIRED)
                    .message("Pass has expired at " + pass.getValidUntil())
                    .barrierAction("HOLD")
                    .timestamp(now)
                    .build();
        }

        if (pass.getStatus() == ExpressPassStatus.COMPLETED) {
            return ExpressVerifyResponse.builder()
                    .allowed(false)
                    .passId(pass.getId())
                    .orderId(pass.getOrderId())
                    .partner(pass.getPartner())
                    .status(ExpressPassStatus.COMPLETED)
                    .message("Delivery order already marked completed / checked out.")
                    .barrierAction("HOLD")
                    .timestamp(now)
                    .build();
        }

        // Grant instantaneous entry & update status
        pass.setStatus(ExpressPassStatus.CHECKED_IN);
        pass.setCheckedInAt(now);
        if (req.getGateId() != null) pass.setEntryGate(req.getGateId());
        passRepository.save(pass);

        return ExpressVerifyResponse.builder()
                .allowed(true)
                .passId(pass.getId())
                .orderId(pass.getOrderId())
                .partner(pass.getPartner())
                .riderName(pass.getRiderName())
                .riderPhone(pass.getRiderPhone())
                .vehicleNumber(pass.getVehicleNumber())
                .flatNumber(pass.getFlatNumber())
                .residentName(pass.getResidentName())
                .leaveAtGate(pass.getLeaveAtGate())
                .gateInstructions(pass.getGateInstructions())
                .status(ExpressPassStatus.CHECKED_IN)
                .barrierAction("OPEN_BOOM_BARRIER")
                .message("PRE-APPROVED: " + pass.getPartner() + " delivery authorized for Flat " + pass.getFlatNumber())
                .timestamp(now)
                .build();
    }

    @Override
    @Transactional
    public ExpressVerifyResponse checkoutExit(ExpressCheckoutRequest req) {
        String id = req.getIdentifier() != null ? req.getIdentifier().trim() : "";
        ExpressDeliveryPass pass = passRepository.findByEntryPassCode(id)
                .or(() -> passRepository.findByBarcodePayload(id))
                .or(() -> {
                    if (!id.startsWith("EXP-") && id.matches("\\d+")) {
                        return passRepository.findByEntryPassCode("EXP-" + id);
                    }
                    return java.util.Optional.empty();
                })
                .orElse(null);

        if (pass == null) {
            return ExpressVerifyResponse.builder()
                    .allowed(false)
                    .message("Pass not found for checkout: " + id)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        pass.setStatus(ExpressPassStatus.COMPLETED);
        pass.setCheckedOutAt(now);
        if (req.getGateId() != null) pass.setExitGate(req.getGateId());
        passRepository.save(pass);

        return ExpressVerifyResponse.builder()
                .allowed(true)
                .passId(pass.getId())
                .orderId(pass.getOrderId())
                .partner(pass.getPartner())
                .riderName(pass.getRiderName())
                .flatNumber(pass.getFlatNumber())
                .status(ExpressPassStatus.COMPLETED)
                .barrierAction("OPEN_BOOM_BARRIER")
                .message("Rider checked out successfully.")
                .timestamp(now)
                .build();
    }

    @Override
    public SocietyGateRulesResponse getSocietyGateRules(Long communityId) {
        return SocietyGateRulesResponse.builder()
                .communityId(communityId)
                .communityName("Mana Community")
                .vehiclesAllowedInside(true)
                .twoWheelerParkingBay("Basement 1, Dedicated Delivery Bays B1-B8")
                .speedLimitKmph(15)
                .quietHours("11:00 PM to 06:00 AM (Deliveries must be left at Gate)")
                .expressDeliveryGates(List.of("Gate 1 (Main Entrance)", "Gate 2 (Service Gate)"))
                .visitorHelpdeskPhone("+91-80-6000-MANA")
                .build();
    }

    @Override
    public ResidentDeliveryPreferenceDto getPreferences(Long communityId, String flatNumber) {
        ResidentDeliveryPreference pref = preferenceRepository
                .findByCommunityIdAndFlatNumber(communityId, flatNumber)
                .orElseGet(() -> ResidentDeliveryPreference.builder()
                        .communityId(communityId)
                        .flatNumber(flatNumber)
                        .autoApproveSwiggy(true)
                        .autoApproveZomato(true)
                        .autoApproveBlinkit(true)
                        .autoApproveZepto(true)
                        .autoApproveAmazon(true)
                        .autoApproveOther(true)
                        .defaultLeaveAtGate(false)
                        .deliveryInstructions("Please drop parcel at doorstep and ring bell once.")
                        .build());

        return mapToDto(pref);
    }

    @Override
    @Transactional
    public ResidentDeliveryPreferenceDto updatePreferences(Long userId, ResidentDeliveryPreferenceDto dto) {
        ResidentDeliveryPreference pref = preferenceRepository
                .findByCommunityIdAndFlatNumber(dto.getCommunityId(), dto.getFlatNumber())
                .orElseGet(() -> ResidentDeliveryPreference.builder()
                        .communityId(dto.getCommunityId())
                        .flatNumber(dto.getFlatNumber())
                        .build());

        pref.setUserId(userId);
        if (dto.getAutoApproveSwiggy() != null) pref.setAutoApproveSwiggy(dto.getAutoApproveSwiggy());
        if (dto.getAutoApproveZomato() != null) pref.setAutoApproveZomato(dto.getAutoApproveZomato());
        if (dto.getAutoApproveBlinkit() != null) pref.setAutoApproveBlinkit(dto.getAutoApproveBlinkit());
        if (dto.getAutoApproveZepto() != null) pref.setAutoApproveZepto(dto.getAutoApproveZepto());
        if (dto.getAutoApproveAmazon() != null) pref.setAutoApproveAmazon(dto.getAutoApproveAmazon());
        if (dto.getAutoApproveOther() != null) pref.setAutoApproveOther(dto.getAutoApproveOther());
        if (dto.getDefaultLeaveAtGate() != null) pref.setDefaultLeaveAtGate(dto.getDefaultLeaveAtGate());
        if (dto.getDeliveryInstructions() != null) pref.setDeliveryInstructions(dto.getDeliveryInstructions());

        pref = preferenceRepository.save(pref);
        return mapToDto(pref);
    }

    @Override
    public List<ExpressEntryResponse> getActiveDeliveriesForCommunity(Long communityId) {
        List<ExpressDeliveryPass> list = passRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                communityId, ExpressPassStatus.PRE_APPROVED);
        list.addAll(passRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                communityId, ExpressPassStatus.CHECKED_IN));

        return list.stream().map(p -> ExpressEntryResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .partner(p.getPartner())
                .entryPassCode(p.getEntryPassCode())
                .barcodePayload(p.getBarcodePayload())
                .status(p.getStatus())
                .flatNumber(p.getFlatNumber())
                .riderName(p.getRiderName())
                .vehicleNumber(p.getVehicleNumber())
                .leaveAtGate(p.getLeaveAtGate())
                .gateInstructions(p.getGateInstructions())
                .validFrom(p.getValidFrom())
                .validUntil(p.getValidUntil())
                .build()).collect(Collectors.toList());
    }

    private boolean isPartnerAutoApproved(ResidentDeliveryPreference pref, DeliveryPartner partner) {
        if (pref == null) return true; // Default fail-open for seamless delivery experience
        return switch (partner) {
            case SWIGGY -> Boolean.TRUE.equals(pref.getAutoApproveSwiggy());
            case ZOMATO -> Boolean.TRUE.equals(pref.getAutoApproveZomato());
            case BLINKIT -> Boolean.TRUE.equals(pref.getAutoApproveBlinkit());
            case ZEPTO -> Boolean.TRUE.equals(pref.getAutoApproveZepto());
            case AMAZON -> Boolean.TRUE.equals(pref.getAutoApproveAmazon());
            default -> Boolean.TRUE.equals(pref.getAutoApproveOther());
        };
    }

    private String generatePassCode() {
        int code = 1000 + random.nextInt(9000);
        return "EXP-" + code;
    }

    private ResidentDeliveryPreferenceDto mapToDto(ResidentDeliveryPreference p) {
        return ResidentDeliveryPreferenceDto.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .communityId(p.getCommunityId())
                .flatNumber(p.getFlatNumber())
                .autoApproveSwiggy(p.getAutoApproveSwiggy())
                .autoApproveZomato(p.getAutoApproveZomato())
                .autoApproveBlinkit(p.getAutoApproveBlinkit())
                .autoApproveZepto(p.getAutoApproveZepto())
                .autoApproveAmazon(p.getAutoApproveAmazon())
                .autoApproveOther(p.getAutoApproveOther())
                .defaultLeaveAtGate(p.getDefaultLeaveAtGate())
                .deliveryInstructions(p.getDeliveryInstructions())
                .build();
    }
}
