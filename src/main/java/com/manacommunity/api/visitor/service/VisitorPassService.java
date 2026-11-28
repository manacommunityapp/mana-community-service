package com.manacommunity.api.visitor.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.exception.InvalidStateTransitionException;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.entity.VisitorAuditLog;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import com.manacommunity.api.visitor.repository.VisitorAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private final com.manacommunity.api.platform.outbox.DomainEventPublisher domainEventPublisher;
    private final com.manacommunity.api.privacy.PiiMaskingService piiMaskingService;
    private final com.manacommunity.api.visitor.repository.GroupInviteRepository groupInviteRepo;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private com.manacommunity.api.visitor.security.OfflineQrTokenService offlineQrTokenService;

    @Autowired(required = false)
    private com.manacommunity.api.delivery.service.OverstaySlaEngineService overstaySlaEngineService;

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
        Optional<AppUser> userOpt = userRepo.findById(residentId);
        if (userOpt.isPresent()) {
            AppUser resident = userOpt.get();
            Long commId = resident.getCommunity() != null ? resident.getCommunity().getId() : null;
            String flatNo = resident.getFlatNo();
            if (commId != null && flatNo != null && !flatNo.isBlank()) {
                return repo.findPendingForResidentOrFlat(residentId, commId, flatNo, VisitorPass.PassStatus.PENDING)
                        .stream().map(this::toResponse).toList();
            }
        }
        return repo.findByResidentIdAndStatusOrderByCreatedAtDesc(residentId, VisitorPass.PassStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Authorizes any verified resident living in the same flat/community to act on
     * a household walk-in approval (prevents 403 when co-residents respond simultaneously).
     */
    public boolean isResidentAuthorizedForPass(AppUser user, VisitorPassResponse pass) {
        if (user == null || pass == null) return false;
        if (pass.getResidentId() != null && pass.getResidentId().equals(user.getId())) {
            return true;
        }
        Long userCommId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (userCommId != null && userCommId.equals(pass.getCommunityId())
                && user.getFlatNo() != null && !user.getFlatNo().isBlank()
                && pass.getFlatNumber() != null
                && user.getFlatNo().trim().equalsIgnoreCase(pass.getFlatNumber().trim())) {
            return true;
        }
        return pass.getResidentId() == null;
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
                .otpHash(hashOtp(otp))  // Store only the hash — never the plain OTP
                .otpExpiresAt(LocalDateTime.now().plusHours(12))
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

        // Audit log — do NOT include OTP
        writeAudit(saved.getId(), saved.getVisitorName(), "CREATED",
                "Resident (" + resident.getFullName() + ")",
                "Pre-approved visitor pass created. QR Token Generated.");

        // Return plain OTP ONCE at creation — all subsequent reads return null
        VisitorPassResponse response = toResponse(saved);
        response.setOtpOnCreation(otp);
        return response;
    }

    @Transactional
    public VisitorPassResponse createWalkIn(VisitorPassRequest req, AppUser guard, Community community) {
        Long commIdHint = community != null
                ? community.getId()
                : (guard != null && guard.getCommunity() != null ? guard.getCommunity().getId() : null);

        // 1. Idempotent retry check: packet-loss retries return the existing PENDING pass
        //    without creating duplicate rows or firing duplicate push/e-Intercom notifications.
        String idempotencyKey = req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()
                ? req.getIdempotencyKey().trim()
                : null;
        if (idempotencyKey != null) {
            Optional<VisitorPass> existing = commIdHint != null
                    ? repo.findByCommunityIdAndIdempotencyKey(commIdHint, idempotencyKey)
                    : repo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent walk-in retry detected key={} passId={}", idempotencyKey, existing.get().getId());
                return toResponse(existing.get());
            }
        }

        AppUser resident;
        List<AppUser> flatResidents = new ArrayList<>();
        if (req.getResidentId() != null) {
            resident = userRepo.findById(req.getResidentId())
                    .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + req.getResidentId()));
            flatResidents.add(resident);
            String resFlat = req.getFlatNumber() != null && !req.getFlatNumber().isBlank()
                    ? req.getFlatNumber().trim()
                    : resident.getFlatNo();
            Long resCommId = commIdHint != null
                    ? commIdHint
                    : (resident.getCommunity() != null ? resident.getCommunity().getId() : null);
            if (resCommId != null && resFlat != null && !resFlat.isBlank()) {
                flatResidents.addAll(userRepo.findByCommunityIdAndFlatNoQuery(resCommId, resFlat));
            }
        } else if (req.getFlatNumber() != null && !req.getFlatNumber().isBlank()) {
            List<AppUser> matches = commIdHint != null
                    ? userRepo.findByCommunityIdAndFlatNoQuery(commIdHint, req.getFlatNumber().trim())
                    : userRepo.findByFlatNoIgnoreCase(req.getFlatNumber().trim());
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("No resident registered for flat: " + req.getFlatNumber());
            }
            resident = matches.get(0);
            flatResidents.addAll(matches);
        } else {
            throw new IllegalArgumentException("Resident ID or Flat Number is required for walk-in approval");
        }

        if (community == null) {
            community = resident.getCommunity();
        }

        String code = generatePassCode();
        String otp = generateOtpCode();

        VisitorPass pass = VisitorPass.builder()
                .passCode(code)
                .idempotencyKey(idempotencyKey)
                .otpHash(hashOtp(otp))  // Store only the hash
                .otpExpiresAt(LocalDateTime.now().plusMinutes(15))
                .visitorName(req.getVisitorName())
                .visitorPhone(req.getVisitorPhone())
                .vehicleNumber(req.getVehicleNumber())
                .purpose(req.getPurpose())
                .passType(parseEnum(VisitorPass.PassType.class, req.getPassType(), VisitorPass.PassType.WALK_IN))
                .flatNumber(req.getFlatNumber() != null ? req.getFlatNumber() : resident.getFlatNo())
                .resident(resident)
                .community(community)
                .status(VisitorPass.PassStatus.PENDING)
                .gateIn(req.getGate())
                .guardIn(guard != null ? guard.getFullName() : "Guard")
                .visitorPhoto(req.getVisitorPhoto())
                .build();

        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "CREATED",
                "Guard (" + (guard != null ? guard.getFullName() : "System") + ")",
                "Walk-in visitor record created. Awaiting Resident approval.");

        // 2. Publish VisitorArrived event via Transactional Outbox for async worker fanout
        try {
            Map<String, Object> arrivalPayload = Map.of(
                    "passId", saved.getId(),
                    "visitorName", saved.getVisitorName() != null ? saved.getVisitorName() : "",
                    "flatNumber", saved.getFlatNumber() != null ? saved.getFlatNumber() : "",
                    "purpose", saved.getPurpose() != null ? saved.getPurpose() : "",
                    "gate", saved.getGateIn() != null ? saved.getGateIn() : ""
            );
            domainEventPublisher.publish("VISITOR_PASS", String.valueOf(saved.getId()),
                    "VisitorArrived", arrivalPayload,
                    saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        } catch (Exception ex) {
            log.warn("Failed to publish VisitorArrived outbox event: {}", ex.getMessage());
        }

        // 3. Fan out high-priority notification to ALL active residents of the flat (deduplicated)
        Set<Long> notifiedUserIds = new LinkedHashSet<>();
        for (AppUser target : flatResidents) {
            if (target == null || target.getId() == null || !notifiedUserIds.add(target.getId())) {
                continue;
            }
            try {
                notificationService.createNotification(
                        target.getId(),
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
                log.error("Failed to send walk-in approval notification to resident {}: {}", target.getId(), e.getMessage());
            }
        }

        // Return plain OTP ONCE for guard to communicate to resident
        VisitorPassResponse response = toResponse(saved);
        response.setOtpOnCreation(otp);
        return response;
    }

    private static final Set<VisitorPass.PassStatus> APPROVE_ALLOWED_SOURCES =
            Set.of(VisitorPass.PassStatus.PENDING);
    private static final Set<VisitorPass.PassStatus> REJECT_ALLOWED_SOURCES =
            Set.of(VisitorPass.PassStatus.PENDING, VisitorPass.PassStatus.EXPECTED);

    @Transactional
    public VisitorPassResponse approvePass(Long id, AppUser resident) {
        String actor = "Resident (" + (resident != null ? resident.getFullName() : "User") + ")";
        return transitionPassStatusAtomically(
                id,
                APPROVE_ALLOWED_SOURCES,
                VisitorPass.PassStatus.APPROVED,
                "RESIDENT_APPROVED",
                actor,
                "Walk-in request APPROVED by resident.");
    }

    @Transactional
    public VisitorPassResponse leaveAtGate(Long id, AppUser resident) {
        String actor = "Resident (" + (resident != null ? resident.getFullName() : "User") + ")";
        return transitionPassStatusAtomically(
                id,
                APPROVE_ALLOWED_SOURCES,
                VisitorPass.PassStatus.LEAVE_AT_GATE,
                "LEAVE_AT_GATE",
                actor,
                "Resident instructed security to hold parcel at gate desk.");
    }

    @Transactional
    public VisitorPassResponse rejectPass(Long id, String performer) {
        String actor = performer != null ? performer : "System";
        return transitionPassStatusAtomically(
                id,
                REJECT_ALLOWED_SOURCES,
                VisitorPass.PassStatus.REJECTED,
                "REJECTED",
                actor,
                "Visitor pass REJECTED / CANCELLED.");
    }

    /**
     * Performs a distributed concurrency-safe Compare-and-Swap (CAS) transition on a visitor pass.
     * Guarantees that if two residents in the same flat tap Approve and Deny on the same
     * millisecond across separate servers, PostgreSQL row-level locking commits one winner
     * and the losing request receives HTTP 409 Conflict via {@link InvalidStateTransitionException}.
     */
    private VisitorPassResponse transitionPassStatusAtomically(
            Long id,
            Set<VisitorPass.PassStatus> allowedSourceStates,
            VisitorPass.PassStatus targetStatus,
            String auditAction,
            String actor,
            String auditNote) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));

        VisitorPass.PassStatus expectedStatus = pass.getStatus();
        if (!allowedSourceStates.contains(expectedStatus)) {
            throw new InvalidStateTransitionException("VisitorPass", expectedStatus.name(), targetStatus.name());
        }

        int updated = repo.compareAndSwapStatus(id, expectedStatus, targetStatus, LocalDateTime.now());
        if (updated == 0) {
            VisitorPass latest = repo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
            throw new InvalidStateTransitionException(
                    "VisitorPass",
                    latest.getStatus() != null ? latest.getStatus().name() : "UNKNOWN",
                    targetStatus.name());
        }

        pass.setStatus(targetStatus);
        writeAudit(pass.getId(), pass.getVisitorName(), auditAction, actor, auditNote);
        VisitorPassResponse response = toResponse(pass);
        broadcastPassDecision(pass, response, actor);
        return response;
    }

    private void broadcastPassDecision(VisitorPass pass, VisitorPassResponse response, String decidedBy) {
        if (messagingTemplate == null || pass.getCommunity() == null) return;
        try {
            Map<String, Object> event = Map.of(
                    "passId", pass.getId(),
                    "status", pass.getStatus().name(),
                    "flatNumber", pass.getFlatNumber() != null ? pass.getFlatNumber() : "",
                    "decidedBy", decidedBy,
                    "pass", response
            );
            messagingTemplate.convertAndSend("/topic/community." + pass.getCommunity().getId() + ".visitors", event);
        } catch (Exception ex) {
            log.warn("Failed to broadcast visitor decision STOMP event for pass {}: {}", pass.getId(), ex.getMessage());
        }
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

        try {
            Map<String, Object> payload = Map.of(
                    "passId", saved.getId(),
                    "visitorName", saved.getVisitorName() != null ? saved.getVisitorName() : "",
                    "residentId", saved.getResident() != null ? saved.getResident().getId() : 0L,
                    "flatNumber", saved.getFlatNumber() != null ? saved.getFlatNumber() : ""
            );
            domainEventPublisher.publish("VISITOR_PASS", String.valueOf(saved.getId()),
                    "VisitorCheckedIn", payload, saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event for visitor check-in: {}", ex.getMessage());
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

        try {
            if (saved.getResident() != null) {
                notificationService.createNotification(
                        saved.getResident().getId(),
                        NotificationType.VISITOR_CHECK_OUT,
                        NotificationCategory.COMMUNITY,
                        "Visitor Checked Out",
                        saved.getVisitorName() + " has checked out of the premises at " + formatTimeOnly(saved.getCheckedOutAt()) + ".",
                        "/visitors",
                        ReferenceType.VISITOR_PASS,
                        saved.getId(),
                        NotificationPriority.NORMAL,
                        null,
                        saved.getCommunity() != null ? saved.getCommunity().getId() : null
                );
            }
        } catch (Exception e) {
            log.error("Failed to send check-out notification to resident: {}", e.getMessage());
        }

        try {
            Map<String, Object> payload = Map.of(
                    "passId", saved.getId(),
                    "visitorName", saved.getVisitorName() != null ? saved.getVisitorName() : "",
                    "residentId", saved.getResident() != null ? saved.getResident().getId() : 0L,
                    "flatNumber", saved.getFlatNumber() != null ? saved.getFlatNumber() : ""
            );
            domainEventPublisher.publish("VISITOR_PASS", String.valueOf(saved.getId()),
                    "VisitorCheckedOut", payload, saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event for visitor check-out: {}", ex.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public void sendOverstayAlert(Long id, String reason) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        if (pass.getResident() != null) {
            try {
                notificationService.createNotification(
                        pass.getResident().getId(),
                        NotificationType.VISITOR_OVERSTAY_ALERT,
                        NotificationCategory.SECURITY,
                        "Visitor Overstay Alert",
                        "Visitor " + pass.getVisitorName() + " has exceeded expected duration." + (reason != null && !reason.isBlank() ? " Note: " + reason : ""),
                        "/visitors",
                        ReferenceType.VISITOR_PASS,
                        pass.getId(),
                        NotificationPriority.HIGH,
                        null,
                        pass.getCommunity() != null ? pass.getCommunity().getId() : null
                );
            } catch (Exception e) {
                log.error("Failed to send overstay alert notification: {}", e.getMessage());
            }
        }
        writeAudit(pass.getId(), pass.getVisitorName(), "OVERSTAY_ALERT", "System/Security",
                "Overstay alert issued for visitor." + (reason != null ? " Reason: " + reason : ""));

        try {
            Map<String, Object> payload = Map.of(
                    "passId", pass.getId(),
                    "visitorName", pass.getVisitorName() != null ? pass.getVisitorName() : "",
                    "residentId", pass.getResident() != null ? pass.getResident().getId() : 0L,
                    "reason", reason != null ? reason : ""
            );
            domainEventPublisher.publish("VISITOR_PASS", String.valueOf(pass.getId()),
                    "VisitorOverstay", payload, pass.getCommunity() != null ? pass.getCommunity().getId() : null);
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event for visitor overstay: {}", ex.getMessage());
        }
    }

    @Transactional
    public void reject(Long id) {
        rejectPass(id, "Guard");
    }

    @Transactional
    public VisitorPassResponse capturePhoto(Long id, String photoUrl) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));
        pass.setVisitorPhoto(photoUrl);
        pass.setPhotoCapturedAt(LocalDateTime.now());
        return toResponse(repo.save(pass));
    }

    @Transactional(readOnly = true)
    public VisitorPassResponse verifyPassCodeOrOtpOrPhone(String codeOrOtpOrPhone, Long communityId) {
        if (codeOrOtpOrPhone == null || codeOrOtpOrPhone.isBlank()) {
            throw new IllegalArgumentException("Search term is empty");
        }

        String search = codeOrOtpOrPhone.trim();
        Optional<VisitorPass> optPass = repo.findByPassCode(search);
        if (optPass.isEmpty()) {
            // OTP lookup: hash the input and compare against stored hash
            optPass = repo.findByOtpHash(hashOtp(search));
        }
        if (optPass.isEmpty()) {
            // Find by phone
            optPass = repo.findFirstByVisitorPhoneAndStatusInOrderByCreatedAtDesc(
                    search, List.of(VisitorPass.PassStatus.APPROVED, VisitorPass.PassStatus.CHECKED_IN, VisitorPass.PassStatus.PENDING));
        }

        if (optPass.isEmpty()) {
            // Check group invites
            Optional<com.manacommunity.api.visitor.entity.GroupInvite> groupOpt = groupInviteRepo.findByPassCode(search);
            if (groupOpt.isPresent()) {
                com.manacommunity.api.visitor.entity.GroupInvite gi = groupOpt.get();
                return VisitorPassResponse.builder()
                        .id(gi.getId())
                        .passCode(gi.getPassCode())
                        .visitorName("Group: " + gi.getTitle() + " (" + gi.getCheckedInCount() + "/" + gi.getTotalGuests() + " arrived)")
                        .purpose(gi.getTitle())
                        .passType("EVENT_GUEST")
                        .status(gi.getStatus().equals("ACTIVE") ? "APPROVED" : gi.getStatus())
                        .residentName(gi.getHost() != null ? gi.getHost().getFullName() : "Host")
                        .flatNumber(gi.getHost() != null ? gi.getHost().getFlatNo() : "N/A")
                        .communityId(gi.getCommunity() != null ? gi.getCommunity().getId() : null)
                        .expectedAt(gi.getEventDate() != null ? gi.getEventDate().toString() : null)
                        .build();
            }
        }

        VisitorPass pass = optPass.orElseThrow(() -> new IllegalArgumentException(
                "No active visitor pass found for the provided code/OTP/phone"));

        // Safety check for expiry
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

        Map<Integer, Integer> hourCounts = new TreeMap<>();
        for (int h = 0; h < 24; h++) hourCounts.put(h, 0);
        for (VisitorPass p : all) {
            if (p.getCheckedInAt() != null) {
                int hr = p.getCheckedInAt().getHour();
                hourCounts.put(hr, hourCounts.get(hr) + 1);
            }
        }

        Map<String, Integer> categories = new HashMap<>();
        for (VisitorPass p : all) {
            String type = p.getPassType() != null ? p.getPassType().name() : "OTHER";
            categories.put(type, categories.getOrDefault(type, 0) + 1);
        }

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

    @Transactional(readOnly = true)
    public Map<String, Object> getApprovalStatus(Long id) {
        VisitorPass pass = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pass not found: " + id));

        String approvalStatus = "PENDING";
        if (pass.getStatus() == VisitorPass.PassStatus.APPROVED || pass.getStatus() == VisitorPass.PassStatus.CHECKED_IN) {
            approvalStatus = "APPROVED";
        } else if (pass.getStatus() == VisitorPass.PassStatus.REJECTED
                || pass.getStatus() == VisitorPass.PassStatus.DENIED
                || pass.getStatus() == VisitorPass.PassStatus.EXPIRED
                || pass.getStatus() == VisitorPass.PassStatus.CANCELLED) {
            approvalStatus = "DENIED";
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", pass.getId());
        res.put("visitorName", pass.getVisitorName());
        res.put("visitorPhone", pass.getVisitorPhone());
        res.put("passCode", pass.getPassCode());
        res.put("flatNumber", pass.getFlatNumber());
        res.put("passType", pass.getPassType() != null ? pass.getPassType().name() : "GUEST");
        res.put("purpose", pass.getPurpose());
        res.put("approvalStatus", approvalStatus);
        res.put("status", pass.getStatus().name());
        res.put("arrivedAt", formatDt(pass.getCreatedAt()));
        res.put("gate", pass.getGateIn());
        res.put("guardName", pass.getGuardIn());
        return res;
    }

    @Transactional
    public Map<String, String> generateDeliveryOtp(String flat, String company, AppUser guard, Community community) {
        if (flat == null || flat.isBlank()) {
            throw new IllegalArgumentException("Flat number is required");
        }
        String cleanFlat = flat.trim();
        Long communityId = community != null ? community.getId() : (guard != null && guard.getCommunity() != null ? guard.getCommunity().getId() : null);

        List<AppUser> residents;
        if (communityId != null) {
            residents = userRepo.findByCommunityIdAndFlatNoQuery(communityId, cleanFlat);
        } else {
            residents = userRepo.findByFlatNoIgnoreCase(cleanFlat);
        }

        if (residents.isEmpty()) {
            throw new IllegalArgumentException("No resident found registered for Flat: " + cleanFlat);
        }

        AppUser resident = residents.get(0);
        Community targetCommunity = community != null ? community : resident.getCommunity();

        String code = generatePassCode();
        String otp = generateOtpCode();
        String companyName = (company != null && !company.isBlank()) ? company.trim() : "Delivery";

        VisitorPass pass = VisitorPass.builder()
                .passCode(code)
                .otpHash(hashOtp(otp))
                .otpExpiresAt(LocalDateTime.now().plusMinutes(15))
                .visitorName(companyName + " Courier")
                .purpose(companyName + " Package Delivery")
                .passType(VisitorPass.PassType.DELIVERY)
                .flatNumber(cleanFlat)
                .resident(resident)
                .community(targetCommunity)
                .status(VisitorPass.PassStatus.PENDING)
                .gateIn(guard != null ? "Gate Desk" : "Main Gate")
                .guardIn(guard != null ? guard.getFullName() : "Security")
                .build();

        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "CREATED",
                "Guard (" + (guard != null ? guard.getFullName() : "Gate") + ")",
                "Delivery OTP issued for " + companyName + " to Flat " + cleanFlat);

        try {
            notificationService.createNotification(
                    resident.getId(),
                    NotificationType.VISITOR_PENDING,
                    NotificationCategory.COMMUNITY,
                    "Delivery Gate OTP",
                    companyName + " delivery arrived at the gate for your flat (" + cleanFlat + "). Share OTP: " + otp + " with the guard, or approve via app.",
                    "/visitors",
                    ReferenceType.VISITOR_PASS,
                    saved.getId(),
                    NotificationPriority.HIGH,
                    null,
                    targetCommunity != null ? targetCommunity.getId() : null
            );
        } catch (Exception e) {
            log.error("Failed to notify resident of delivery OTP: {}", e.getMessage());
        }

        String phone = resident.getPhone();
        String masked = piiMaskingService.maskPhone(phone);
        return Map.of(
                "maskedPhone", masked != null ? masked : "Registered Phone",
                "passCode", code
        );
    }

    @Transactional
    public VisitorPassResponse verifyDeliveryOtp(String flat, String otp, AppUser guard, Community community) {
        if (flat == null || flat.isBlank() || otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("Flat and OTP are required");
        }
        String cleanFlat = flat.trim();
        String cleanOtp = otp.trim();
        String hashed = hashOtp(cleanOtp);

        Optional<VisitorPass> optPass = repo.findByOtpHash(hashed);
        if (optPass.isEmpty()) {
            throw new IllegalArgumentException("Invalid OTP for delivery verification");
        }

        VisitorPass pass = optPass.get();
        if (pass.getOtpExpiresAt() != null && LocalDateTime.now().isAfter(pass.getOtpExpiresAt())) {
            pass.setStatus(VisitorPass.PassStatus.EXPIRED);
            repo.save(pass);
            throw new IllegalArgumentException("Delivery OTP has expired. Please request a new one.");
        }

        pass.setStatus(VisitorPass.PassStatus.CHECKED_IN);
        pass.setCheckedInAt(LocalDateTime.now());
        if (guard != null) pass.setGuardIn(guard.getFullName());
        VisitorPass saved = repo.save(pass);

        writeAudit(saved.getId(), saved.getVisitorName(), "ENTRY_COMPLETED",
                guard != null ? "Guard (" + guard.getFullName() + ")" : "Guard",
                "Delivery OTP verified. Visitor entered for Flat " + cleanFlat);

        try {
            if (saved.getResident() != null) {
                notificationService.createNotification(
                        saved.getResident().getId(),
                        NotificationType.VISITOR_CHECK_IN,
                        NotificationCategory.COMMUNITY,
                        "Delivery Verified & Entered",
                        saved.getVisitorName() + " has entered the community to deliver to your flat (" + cleanFlat + ").",
                        "/visitors",
                        ReferenceType.VISITOR_PASS,
                        saved.getId(),
                        NotificationPriority.NORMAL,
                        null,
                        saved.getCommunity() != null ? saved.getCommunity().getId() : null
                );
            }
        } catch (Exception e) {
            log.error("Failed to notify resident of delivery check-in: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${app.visitor.overstay-cron:0 */15 * * * *}")
    @Transactional
    public void autoDetectOverstays() {
        LocalDateTime now = LocalDateTime.now();
        List<VisitorPass> checkedIn = repo.findAll().stream()
                .filter(p -> p.getStatus() == VisitorPass.PassStatus.CHECKED_IN)
                .filter(p -> p.getCheckedInAt() != null)
                .toList();

        for (VisitorPass p : checkedIn) {
            try {
                if (overstaySlaEngineService != null) {
                    var eval = overstaySlaEngineService.evaluateVisitorPass(p, now);
                    if (!eval.overstayed()) {
                        continue;
                    }
                    if (eval.action() == com.manacommunity.api.delivery.service.OverstaySlaEngineService.OverstayAction.RESIDENT_CONFIRMATION_PING) {
                        if (p.getResident() != null) {
                            notificationService.createNotification(
                                    p.getResident().getId(),
                                    NotificationType.VISITOR_OVERSTAY_ALERT,
                                    NotificationCategory.COMMUNITY,
                                    "Confirm Ongoing Service Visit",
                                    eval.reason(),
                                    "/visitors",
                                    ReferenceType.VISITOR_PASS,
                                    p.getId(),
                                    NotificationPriority.NORMAL,
                                    null,
                                    p.getCommunity() != null ? p.getCommunity().getId() : null
                            );
                        }
                        writeAudit(p.getId(), p.getVisitorName(), "RESIDENT_CONFIRMATION_PING", "System/SLA", eval.reason());
                    } else {
                        sendOverstayAlert(p.getId(), eval.reason());
                    }
                } else {
                    LocalDateTime cutoff = now.minusHours(12);
                    if (p.getCheckedInAt().isBefore(cutoff)) {
                        sendOverstayAlert(p.getId(), "Automated check: visitor checked in > 12 hours ago without check-out record.");
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to auto-send overstay alert for pass {}: {}", p.getId(), ex.getMessage());
            }
        }
    }

    // ── OTP Helpers ──────────────────────────────────────────────────────────

    /** Generates a cryptographically random 6-digit OTP. */
    private String generateOtpCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    /** SHA-256 hash of the OTP for secure storage. */
    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    // ── Other Helpers ─────────────────────────────────────────────────────────

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

    private String generateEncryptedToken(String code, AppUser user) {
        try {
            if (offlineQrTokenService != null) {
                Long commId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : 0L;
                String flat = user != null ? user.getFlatNo() : "N/A";
                return offlineQrTokenService.signToken(commId, code, flat, 43_200L);
            }
            String raw = "VISITOR:" + code + "|FLAT:" + (user != null ? user.getFlatNo() : "N/A") + "|GEN:" + System.currentTimeMillis();
            return Base64.getEncoder().encodeToString(raw.getBytes());
        } catch (Exception e) {
            return "TOKEN-" + code;
        }
    }

    public String getOfflineQrPublicKeyBase64() {
        return offlineQrTokenService != null ? offlineQrTokenService.getPublicKeyBase64() : "";
    }

    /**
     * Converts a VisitorPass entity to a response DTO.
     * The {@code otpOnCreation} field is always null here — it is set only in
     * {@link #create} and {@link #createWalkIn} immediately after saving.
     */
    private VisitorPassResponse toResponse(VisitorPass p) {
        return VisitorPassResponse.builder()
                .id(p.getId())
                .passCode(p.getPassCode())
                .idempotencyKey(p.getIdempotencyKey())
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
                // OTP is NEVER included in reads — set to null always
                .otpOnCreation(null)
                .otpExpiresAt(formatDt(p.getOtpExpiresAt()))
                .gateIn(p.getGateIn())
                .gateOut(p.getGateOut())
                .guardIn(p.getGuardIn())
                .guardOut(p.getGuardOut())
                .visitorPhoto(p.getVisitorPhoto())
                // encryptedToken is NEVER returned in responses
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
