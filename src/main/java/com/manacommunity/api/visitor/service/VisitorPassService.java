package com.manacommunity.api.visitor.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.entity.VisitorAuditLog;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import com.manacommunity.api.visitor.repository.VisitorAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorPassService {

    private final VisitorPassRepository repo;
    private final VisitorAuditLogRepository auditLogRepo;
    private final AppUserRepository userRepo;
    private final NotificationManagementService notificationService;

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getCommunityPasses(Long communityId) {
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getActivePasses(Long communityId) {
        return repo.findByCommunityAndStatuses(communityId,
                        List.of(VisitorPass.PassStatus.APPROVED, VisitorPass.PassStatus.CHECKED_IN))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getMyPasses(Long residentId) {
        return repo.findByResidentIdOrderByCreatedAtDesc(residentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getPendingApprovals(Long residentId) {
        return repo.findByResidentIdAndStatusOrderByCreatedAtDesc(residentId, VisitorPass.PassStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VisitorPassResponse getByPassCode(String passCode) {
        return toResponse(repo.findByPassCode(passCode)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + passCode)));
    }

    @Transactional(readOnly = true)
    public VisitorPassResponse getById(Long id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<VisitorPassResponse> getTodaysPasses(Long communityId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return repo.findRecentByCommunity(communityId, startOfDay)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorAuditLog> getRecentAuditLogs() {
        return auditLogRepo.findTop50ByOrderByTimestampDesc();
    }

    @Transactional
    public VisitorPassResponse create(VisitorPassRequest req, AppUser resident, Community community) {
        String code = generatePassCode();
        String otp = generateOtpCode();
        
        VisitorPass pass = VisitorPass.builder()
                .passCode(code)
                .otp(otp)
                .otpExpiresAt(LocalDateTime.now().plusHours(12)) // Pre-approved codes valid for 12 hours
                .visitorName(req.getVisitorName())
                .visitorPhone(req.getVisitorPhone())
                .vehicleNumber(req.getVehicleNumber())
                .purpose(req.getPurpose())
                .passType(parseEnum(VisitorPass.PassType.class, req.getPassType(), VisitorPass.PassType.GUEST))
                .flatNumber(req.getFlatNumber() != null ? req.getFlatNumber() : (resident != null ? resident.getFlatNo() : null))
                .resident(resident)
                .community(community)
                .status(VisitorPass.PassStatus.APPROVED)
                .encryptedToken(generateEncryptedToken(code, resident))
                .build();

        if (req.getExpectedAt() != null && !req.getExpectedAt().isBlank()) {
            pass.setExpectedAt(LocalDateTime.parse(req.getExpectedAt()));
        } else {
            pass.setExpectedAt(LocalDateTime.now());
        }

        VisitorPass saved = repo.save(pass);

        // Write Audit log
        writeAudit(saved.getId(), saved.getVisitorName(), "CREATED", 
                "Resident (" + resident.getFullName() + ")", 
                "Pre-approved visitor pass created. OTP: " + otp + ". QR Token Generated.");

        return toResponse(saved);
    }

    @Transactional
    public VisitorPassResponse createWalkIn(VisitorPassRequest req, AppUser guard, Community community) {
        if (req.getResidentId() == null) {
            throw new IllegalArgumentException("Resident ID is required for walk-in approval");
        }
        AppUser resident = userRepo.findById(req.getResidentId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + req.getResidentId()));

        String code = generatePassCode();
        String otp = generateOtpCode();

        VisitorPass pass = VisitorPass.builder()
                .passCode(code)
                .otp(otp)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(15)) // Walk-ins expire in 15 mins if not approved
                .visitorName(req.getVisitorName())
                .visitorPhone(req.getVisitorPhone())
                .vehicleNumber(req.getVehicleNumber())
                .purpose(req.getPurpose())
                .passType(parseEnum(VisitorPass.PassType.class, req.getPassType(), VisitorPass.PassType.WALK_IN))
                .flatNumber(req.getFlatNumber() != null ? req.getFlatNumber() : resident.getFlatNo())
                .resident(resident)
                .community(community)
                .status(VisitorPass.PassStatus.PENDING) // Pending resident approval
                .gateIn(req.getGate())
                .guardIn(guard != null ? guard.getFullName() : "Guard")
                .visitorPhoto(req.getVisitorPhoto())
                .build();

        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "CREATED",
                "Guard (" + (guard != null ? guard.getFullName() : "System") + ")",
                "Walk-in visitor record created. Awaiting Resident approval.");

        // Send simulated Resident Notification
        try {
            notificationService.createNotification(
                    resident.getId(),
                    NotificationType.VISITOR_PENDING,
                    NotificationCategory.COMMUNITY,
                    "Visitor Approval Request",
                    saved.getVisitorName() + " wants to visit your flat (" + saved.getFlatNumber() + "). Purpose: " + saved.getPurpose(),
                    "/visitors",
                    ReferenceType.VISITOR_PASS,
                    saved.getId(),
                    NotificationPriority.HIGH,
                    null,
                    saved.getCommunity() != null ? saved.getCommunity().getId() : null
            );
        } catch (Exception e) {
            log.error("Failed to send walk-in approval notification to resident: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public VisitorPassResponse approvePass(Long id, AppUser resident) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        
        pass.setStatus(VisitorPass.PassStatus.APPROVED);
        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "RESIDENT_APPROVED",
                "Resident (" + (resident != null ? resident.getFullName() : "User") + ")",
                "Walk-in request APPROVED by resident.");

        return toResponse(saved);
    }

    @Transactional
    public VisitorPassResponse rejectPass(Long id, String performer) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        
        pass.setStatus(VisitorPass.PassStatus.REJECTED);
        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "REJECTED",
                performer != null ? performer : "System",
                "Visitor pass REJECTED / CANCELLED.");

        return toResponse(saved);
    }

    @Transactional
    public VisitorPassResponse checkIn(Long id) {
        return checkIn(id, null, null, null);
    }

    @Transactional
    public VisitorPassResponse checkInByCode(String passCode) {
        VisitorPass pass = repo.findByPassCode(passCode)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + passCode));
        return checkIn(pass.getId(), null, null, null);
    }

    @Transactional
    public VisitorPassResponse checkIn(Long id, String gate, String guard, String photo) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));

        if (pass.getStatus() != VisitorPass.PassStatus.APPROVED) {
            throw new IllegalStateException("Pass is not in APPROVED status. Current status: " + pass.getStatus());
        }

        pass.setStatus(VisitorPass.PassStatus.CHECKED_IN);
        pass.setCheckedInAt(LocalDateTime.now());
        if (gate != null) pass.setGateIn(gate);
        if (guard != null) pass.setGuardIn(guard);
        if (photo != null) pass.setVisitorPhoto(photo);

        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "ENTRY_COMPLETED",
                guard != null ? "Guard (" + guard + ")" : "Guard",
                "Visitor check-in successful at " + (gate != null ? gate : "Gate") + ".");

        // Send checked-in notification to Resident
        try {
            if (saved.getResident() != null) {
                notificationService.createNotification(
                        saved.getResident().getId(),
                        NotificationType.VISITOR_CHECK_IN,
                        NotificationCategory.COMMUNITY,
                        "Visitor Entered",
                        saved.getVisitorName() + " entered the community at " + formatTimeOnly(saved.getCheckedInAt()) + ".",
                        "/visitors",
                        ReferenceType.VISITOR_PASS,
                        saved.getId(),
                        NotificationPriority.NORMAL,
                        null,
                        saved.getCommunity() != null ? saved.getCommunity().getId() : null
                );
            }
        } catch (Exception e) {
            log.error("Failed to send check-in notification to resident: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public VisitorPassResponse checkOut(Long id) {
        return checkOut(id, null, null);
    }

    @Transactional
    public VisitorPassResponse checkOut(Long id, String gate, String guard) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));

        if (pass.getStatus() != VisitorPass.PassStatus.CHECKED_IN) {
            throw new IllegalStateException("Visitor has not checked in. Current status: " + pass.getStatus());
        }

        pass.setStatus(VisitorPass.PassStatus.CHECKED_OUT);
        pass.setCheckedOutAt(LocalDateTime.now());
        if (gate != null) pass.setGateOut(gate);
        if (guard != null) pass.setGuardOut(guard);

        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "EXIT_COMPLETED",
                guard != null ? "Guard (" + guard + ")" : "Guard",
                "Visitor check-out successful at " + (gate != null ? gate : "Gate") + ".");

        return toResponse(saved);
    }

    @Transactional
    public void reject(Long id) {
        rejectPass(id, "Guard");
    }

    @Transactional(readOnly = true)
    public VisitorPassResponse verifyPassCodeOrOtpOrPhone(String codeOrOtpOrPhone, Long communityId) {
        if (codeOrOtpOrPhone == null || codeOrOtpOrPhone.isBlank()) {
            throw new IllegalArgumentException("Search term is empty");
        }

        String search = codeOrOtpOrPhone.trim();
        Optional<VisitorPass> optPass = repo.findByPassCode(search);
        if (optPass.isEmpty()) {
            optPass = repo.findByOtp(search);
        }
        if (optPass.isEmpty()) {
            // Find by phone
            optPass = repo.findFirstByVisitorPhoneAndStatusInOrderByCreatedAtDesc(
                    search, List.of(VisitorPass.PassStatus.APPROVED, VisitorPass.PassStatus.CHECKED_IN, VisitorPass.PassStatus.PENDING));
        }

        VisitorPass pass = optPass.orElseThrow(() -> new IllegalArgumentException("No active visitor pass found for code/OTP/phone: " + search));
        
        // Safety checks for expiry
        if (pass.getStatus() == VisitorPass.PassStatus.APPROVED && pass.getOtpExpiresAt() != null) {
            if (LocalDateTime.now().isAfter(pass.getOtpExpiresAt())) {
                pass.setStatus(VisitorPass.PassStatus.EXPIRED);
                repo.save(pass);
            }
        }

        return toResponse(pass);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(Long communityId) {
        List<VisitorPass> all = repo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        
        int total = all.size();
        long inside = all.stream().filter(p -> p.getStatus() == VisitorPass.PassStatus.CHECKED_IN).count();
        long pending = all.stream().filter(p -> p.getStatus() == VisitorPass.PassStatus.PENDING).count();
        long approved = all.stream().filter(p -> p.getStatus() == VisitorPass.PassStatus.APPROVED).count();

        // Visits per day (last 7 days)
        Map<String, Integer> dailyCounts = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            String dateKey = LocalDateTime.now().minusDays(i).toLocalDate().toString();
            dailyCounts.put(dateKey, 0);
        }
        for (VisitorPass p : all) {
            if (p.getCreatedAt() != null) {
                String dateKey = p.getCreatedAt().toLocalDate().toString();
                if (dailyCounts.containsKey(dateKey)) {
                    dailyCounts.put(dateKey, dailyCounts.get(dateKey) + 1);
                }
            }
        }

        // Peak Hours (group check-in hours)
        Map<Integer, Integer> hourCounts = new TreeMap<>();
        for (int h = 0; h < 24; h++) hourCounts.put(h, 0);
        for (VisitorPass p : all) {
            if (p.getCheckedInAt() != null) {
                int hr = p.getCheckedInAt().getHour();
                hourCounts.put(hr, hourCounts.get(hr) + 1);
            }
        }

        // Category breakdown
        Map<String, Integer> categories = new HashMap<>();
        for (VisitorPass p : all) {
            String type = p.getPassType() != null ? p.getPassType().name() : "OTHER";
            categories.put(type, categories.getOrDefault(type, 0) + 1);
        }

        // Top flats visited
        Map<String, Integer> topFlats = new HashMap<>();
        for (VisitorPass p : all) {
            if (p.getFlatNumber() != null && !p.getFlatNumber().isBlank()) {
                topFlats.put(p.getFlatNumber(), topFlats.getOrDefault(p.getFlatNumber(), 0) + 1);
            }
        }
        List<Map.Entry<String, Integer>> flatList = new ArrayList<>(topFlats.entrySet());
        flatList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map<String, Object>> flatRanks = flatList.stream().limit(5).map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("flat", e.getKey());
            m.put("count", e.getValue());
            return m;
        }).toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPasses", total);
        stats.put("currentlyInside", inside);
        stats.put("pendingApprovals", pending);
        stats.put("awaitingEntry", approved);
        stats.put("dailyVisits", dailyCounts);
        stats.put("hourlyVisits", hourCounts);
        stats.put("categoryVisits", categories);
        stats.put("topVisitedFlats", flatRanks);

        return stats;
    }

    private void writeAudit(Long passId, String name, String action, String user, String details) {
        try {
            auditLogRepo.save(VisitorAuditLog.builder()
                    .visitorPassId(passId)
                    .visitorName(name)
                    .action(action)
                    .performedBy(user)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write visitor audit log: {}", e.getMessage());
        }
    }

    private String generatePassCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateOtpCode() {
        return String.format("%06d", new java.util.Random().nextInt(1000000));
    }

    private String generateEncryptedToken(String code, AppUser user) {
        try {
            String raw = "VISITOR:" + code + "|FLAT:" + (user != null ? user.getFlatNo() : "N/A") + "|GEN:" + System.currentTimeMillis();
            return Base64.getEncoder().encodeToString(raw.getBytes());
        } catch (Exception e) {
            return "TOKEN-" + code;
        }
    }

    private VisitorPassResponse toResponse(VisitorPass p) {
        return VisitorPassResponse.builder()
                .id(p.getId())
                .passCode(p.getPassCode())
                .visitorName(p.getVisitorName())
                .visitorPhone(p.getVisitorPhone())
                .vehicleNumber(p.getVehicleNumber())
                .purpose(p.getPurpose())
                .passType(p.getPassType() != null ? p.getPassType().name() : null)
                .status(p.getStatus().name())
                .expectedAt(formatDt(p.getExpectedAt()))
                .checkedInAt(formatDt(p.getCheckedInAt()))
                .checkedOutAt(formatDt(p.getCheckedOutAt()))
                .flatNumber(p.getFlatNumber())
                .residentId(p.getResident() != null ? p.getResident().getId() : null)
                .residentName(p.getResident() != null ? p.getResident().getFullName() : "Walk-In")
                .communityId(p.getCommunity() != null ? p.getCommunity().getId() : null)
                .createdAt(formatDt(p.getCreatedAt()))
                
                // Enhanced columns mapping
                .otp(p.getOtp())
                .otpExpiresAt(formatDt(p.getOtpExpiresAt()))
                .gateIn(p.getGateIn())
                .gateOut(p.getGateOut())
                .guardIn(p.getGuardIn())
                .guardOut(p.getGuardOut())
                .visitorPhoto(p.getVisitorPhoto())
                .encryptedToken(p.getEncryptedToken())
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private String formatTimeOnly(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ofPattern("hh:mm a")) : "";
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E defaultVal) {
        if (value == null || value.isBlank()) return defaultVal;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }
}
