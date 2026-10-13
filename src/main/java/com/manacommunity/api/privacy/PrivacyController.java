package com.manacommunity.api.privacy;

import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.privacy.dto.DataDeletionRequestDto;
import com.manacommunity.api.privacy.dto.DataRetentionPolicyDto;
import com.manacommunity.api.privacy.dto.UserDataExportDto;
import com.manacommunity.api.privacy.dto.UserPrivacySettingsDto;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/privacy", "/privacy", "/api/api/privacy", "/api/v1/privacy"})
@RequiredArgsConstructor
public class PrivacyController {

    private final UserPrivacySettingsService privacySettingsService;
    private final DataDeletionService dataDeletionService;
    private final UserDataExportService dataExportService;
    private final DataRetentionService retentionService;
    private final LoggedInUserService loggedInUserService;
    private final ConsentRecordRepository consentRepo;
    private final DataRightsRequestRepository rightsRepo;
    private final EmailService emailService;
    private final DataRightsRateLimiter rateLimiter;

    /** [LEGAL REVIEW REQUIRED] Replace with monitored DPO mailbox. */
    @Value("${app.privacy.dpo-email:dpo@manacommunity.app}")
    private String dpoEmail;

    /** Current notice version — bump when privacy notice content changes. */
    private static final String NOTICE_VERSION = "2026-10-13-v1";

    // ── Privacy Settings ───────────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<UserPrivacySettingsDto> getMyPrivacySettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(privacySettingsService.getSettings(user.getId()));
    }

    @PutMapping("/settings")
    public ResponseEntity<UserPrivacySettingsDto> updateMyPrivacySettings(
            @Valid @RequestBody UserPrivacySettingsDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(privacySettingsService.updateSettings(user.getId(), dto));
    }

    // ── Consents (persisted) ────────────────────────────────────────────────

    private static final List<Map<String, Object>> CONSENT_CATALOG = List.of(
        Map.of("id", "ESSENTIAL",          "type", "ESSENTIAL",          "title", "Essential Society Operations",      "required", true,  "description", "Gate access, visitor verification, and emergency alerts. Required for platform use."),
        Map.of("id", "PUSH_NOTIFICATIONS", "type", "PUSH_NOTIFICATIONS", "title", "Push & SMS Notifications",          "required", false, "description", "Receive event reminders, payment alerts, and visitor notifications via app push or SMS."),
        Map.of("id", "MARKETING",          "type", "MARKETING",          "title", "Community Offers & Promotions",     "required", false, "description", "Deals and group-buying discounts tailored for society members."),
        Map.of("id", "ANALYTICS",          "type", "ANALYTICS",          "title", "Usage Analytics & Performance",     "required", false, "description", "Anonymous in-app telemetry to improve speed and stability. No data leaves our servers."),
        Map.of("id", "THIRD_PARTY_SHARING","type", "THIRD_PARTY_SHARING","title", "Partner Home Services",             "required", false, "description", "Share your contact with verified plumbing/electrical vendors when you raise a booking."),
        Map.of("id", "BIOMETRIC",          "type", "BIOMETRIC",          "title", "Biometric Gate Access",             "required", false, "description", "Enrol fingerprint/face for touchless turnstile access. Biometric template is hashed; raw image is not retained.")
    );

    @GetMapping("/consents")
    public ResponseEntity<List<Map<String, Object>>> getMyConsents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<ConsentRecord> latest = consentRepo.findLatestPerPurposeForUser(user.getId());
        Map<String, Boolean> stateMap = latest.stream()
                .collect(Collectors.toMap(ConsentRecord::getConsentPurpose, ConsentRecord::getGranted));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> catalog : CONSENT_CATALOG) {
            String id = (String) catalog.get("id");
            boolean defaultGranted = "ESSENTIAL".equals(id);
            boolean granted = stateMap.getOrDefault(id, defaultGranted);
            Map<String, Object> row = new LinkedHashMap<>(catalog);
            row.put("granted", granted);
            row.put("updatedAt", stateMap.containsKey(id)
                    ? latest.stream().filter(c -> id.equals(c.getConsentPurpose()))
                            .max(Comparator.comparing(ConsentRecord::getCreatedAt))
                            .map(c -> c.getCreatedAt().toString()).orElse(null)
                    : null);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/consents")
    public ResponseEntity<?> updateConsent(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        String purpose = Objects.toString(body.get("purpose"), "").toUpperCase();
        boolean granted = Boolean.parseBoolean(Objects.toString(body.get("granted"), "false"));

        if (purpose.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "purpose is required"));
        }
        if ("ESSENTIAL".equals(purpose) && !granted) {
            return ResponseEntity.badRequest().body(Map.of("error", "Essential consent cannot be revoked"));
        }

        ConsentRecord record = ConsentRecord.builder()
                .userId(user.getId())
                .consentPurpose(purpose)
                .granted(granted)
                .sourceIp(extractIp(request))
                .collectionPoint((String) body.getOrDefault("collectionPoint", "SETTINGS"))
                .noticeVersion(NOTICE_VERSION)
                .build();
        consentRepo.save(record);

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "purpose", purpose, "granted", granted));
    }

    @DeleteMapping("/consents/{purpose}")
    public ResponseEntity<?> revokeConsent(
            @PathVariable String purpose,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        if ("ESSENTIAL".equalsIgnoreCase(purpose)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Essential consent cannot be revoked"));
        }
        ConsentRecord record = ConsentRecord.builder()
                .userId(user.getId())
                .consentPurpose(purpose.toUpperCase())
                .granted(false)
                .sourceIp(extractIp(request))
                .collectionPoint("SETTINGS")
                .noticeVersion(NOTICE_VERSION)
                .build();
        consentRepo.save(record);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Consent revoked for: " + purpose));
    }

    // ── Data Portability / View My Data ────────────────────────────────────

    @GetMapping("/my-data")
    public ResponseEntity<UserDataExportDto> getMyData(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dataExportService.exportUserData(user.getId()));
    }

    // ── Anonymization & Audit Log ──────────────────────────────────────────

    @PostMapping("/anonymize")
    public ResponseEntity<?> requestAnonymization(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("status", "ANONYMIZED", "userId", user.getId(), "message", "User PII scrubbed from public indexes"));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<ConsentRecord> records = consentRepo.findByUserId(user.getId());
        List<Map<String, Object>> logs = records.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "action", r.getGranted() ? "CONSENT_GRANTED" : "CONSENT_REVOKED",
                        "purpose", r.getConsentPurpose(),
                        "actor", user.getEmail(),
                        "timestamp", r.getCreatedAt().toString(),
                        "collectionPoint", r.getCollectionPoint() != null ? r.getCollectionPoint() : "",
                        "noticeVersion", r.getNoticeVersion() != null ? r.getNoticeVersion() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    // ── Data-Rights Requests (DPDP s.12–13) ──────────────────────────────

    /**
     * Grievance officer contact — published per DPDP Act s.13.
     * [LEGAL REVIEW REQUIRED] Update with actual DPO name, address, response timeline.
     */
    @GetMapping("/grievance-contact")
    public ResponseEntity<?> grievanceContact() {
        return ResponseEntity.ok(Map.of(
            "grievanceOfficer",    "Data Protection Officer",
            "organisation",        "Mana Community Pvt. Ltd.",
            "email",               "dpo@manacommunity.app",
            "address",             "[LEGAL REVIEW REQUIRED: Insert registered office address]",
            "responseTimelineDays", 30,
            "noticeVersion",       NOTICE_VERSION,
            "legalNote",           "LEGAL REVIEW REQUIRED: Verify DPO details, registered address, and 30-day resolution timeline comply with DPDP Act 2023 s.13 and applicable DPDP Rules."
        ));
    }

    @PostMapping("/data-rights")
    public ResponseEntity<?> submitDataRightsRequest(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        // DPDP — rate-limit to prevent bulk/automated rights requests.
        rateLimiter.checkAndRecord(user.getId());

        String rightType = Objects.toString(body.get("rightType"), "").toUpperCase();
        String description = body.get("description");

        if (rightType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "rightType is required"));
        }
        List<String> validTypes = List.of("ACCESS", "CORRECTION", "ERASURE", "PORTABILITY", "GRIEVANCE", "NOMINATION");
        if (!validTypes.contains(rightType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid rightType. Must be one of: " + validTypes));
        }

        DataRightsRequest req = DataRightsRequest.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .rightType(rightType)
                .description(description)
                .correctionDetails(body.get("correctionDetails"))
                .build();
        DataRightsRequest saved = rightsRepo.save(req);

        // DPDP Act 2023 s.13 — acknowledge the data principal within 72 hours.
        sendDataRightsAcknowledgement(user, saved);
        // Notify DPO so they can begin processing within the 30-day window.
        sendDpoDataRightsNotification(user, saved);

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "status", saved.getStatus(),
                "rightType", saved.getRightType(),
                "dueBy", saved.getDueBy().toString(),
                "message", "Request submitted. We will respond within 30 days per DPDP Act 2023 s.13."
        ));
    }

    @GetMapping("/data-rights")
    public ResponseEntity<?> getMyDataRightsRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(rightsRepo.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @GetMapping("/admin/data-rights")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<?> getAdminDataRightsRequests() {
        return ResponseEntity.ok(rightsRepo.findByStatusOrderByCreatedAtAsc("PENDING"));
    }

    @PostMapping("/admin/data-rights/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<?> processDataRightsRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser admin = loggedInUserService.resolve(principal);
        DataRightsRequest req = rightsRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
        req.setStatus(Objects.toString(body.get("status"), "COMPLETED").toUpperCase());
        req.setAdminNotes(body.get("notes"));
        req.setProcessedBy(admin.getId());
        return ResponseEntity.ok(rightsRepo.save(req));
    }

    // ── Data Deletion Requests ─────────────────────────────────────────────

    @PostMapping("/deletion-request")
    public ResponseEntity<DataDeletionRequestDto> requestAccountDeletion(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = (body != null) ? body.get("reason") : null;
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dataDeletionService.submitRequest(user.getId(), reason, communityId));
    }

    @GetMapping("/deletion-request/status")
    public ResponseEntity<List<DataDeletionRequestDto>> getMyDeletionRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dataDeletionService.getUserRequests(user.getId()));
    }

    @PostMapping("/deletion-request/cancel")
    public ResponseEntity<DataDeletionRequestDto> cancelAccountDeletion(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long requestId = null;
        if (body != null && body.containsKey("requestId") && body.get("requestId") != null) {
            requestId = Long.valueOf(body.get("requestId").toString());
        }
        return ResponseEntity.ok(dataDeletionService.cancelRequest(requestId, user.getId()));
    }

    @GetMapping("/admin/deletion-requests")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<DataDeletionRequestDto>> getAdminDeletionRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dataDeletionService.getCommunityRequests(communityId));
    }

    @PostMapping("/admin/deletion-requests/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<DataDeletionRequestDto> processDeletionRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String notes = (body != null) ? body.get("notes") : null;
        return ResponseEntity.ok(dataDeletionService.processDeletion(id, user.getId(), notes));
    }

    @PostMapping("/admin/deletion-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<DataDeletionRequestDto> rejectDeletionRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String notes = (body != null) ? body.get("notes") : null;
        return ResponseEntity.ok(dataDeletionService.rejectRequest(id, user.getId(), notes));
    }

    // ── Retention Policies ─────────────────────────────────────────────────

    @GetMapping("/admin/retention-policies")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<DataRetentionPolicyDto>> getRetentionPolicies(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(retentionService.getPolicies(communityId));
    }

    @PutMapping("/admin/retention-policies/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<DataRetentionPolicyDto> updateRetentionPolicy(
            @PathVariable Long id,
            @RequestBody DataRetentionPolicyDto dto) {
        return ResponseEntity.ok(retentionService.updatePolicy(id, dto));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * DPDP Act 2023 s.13 — acknowledge the data principal's rights request.
     * Must be sent within 72 hours of receipt. [LEGAL REVIEW REQUIRED]
     */
    private void sendDataRightsAcknowledgement(AppUser user, DataRightsRequest req) {
        String subject = "Your data rights request has been received — Ref #" + req.getId();
        String html = "<div style='font-family:sans-serif;max-width:600px;'>"
            + "<h2 style='color:#4f46e5;'>Data Rights Request Acknowledged</h2>"
            + "<p>Dear " + user.getFullName() + ",</p>"
            + "<p>We have received your <strong>" + req.getRightType() + "</strong> request (Reference #" + req.getId() + ") "
            + "submitted under the <strong>Digital Personal Data Protection (DPDP) Act 2023</strong>.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>"
            + "<tr><td style='padding:8px;background:#f8fafc;font-weight:600;width:40%;'>Request Type</td>"
            + "<td style='padding:8px;'>" + req.getRightType() + "</td></tr>"
            + "<tr><td style='padding:8px;background:#f8fafc;font-weight:600;'>Reference #</td>"
            + "<td style='padding:8px;'>" + req.getId() + "</td></tr>"
            + "<tr><td style='padding:8px;background:#f8fafc;font-weight:600;'>Submitted</td>"
            + "<td style='padding:8px;'>" + req.getCreatedAt().toLocalDate() + "</td></tr>"
            + "<tr><td style='padding:8px;background:#f8fafc;font-weight:600;'>Response Due By</td>"
            + "<td style='padding:8px;'><strong>" + req.getDueBy().toLocalDate() + "</strong></td></tr>"
            + "</table>"
            + "<p>We will respond to your request within <strong>30 days</strong> as required under DPDP Act 2023 s.13.</p>"
            + "<p>If you have any questions, please contact our Grievance Officer at "
            + "<a href='mailto:" + dpoEmail + "'>" + dpoEmail + "</a>.</p>"
            + "<p style='color:#64748b;font-size:12px;margin-top:24px;'>"
            + "This is an automated acknowledgement from Mana Community Privacy Centre. "
            + "Please do not reply to this email. [LEGAL REVIEW REQUIRED: verify this notice meets DPDP s.13]</p>"
            + "</div>";
        emailService.send(new EmailMessage(user.getEmail(), user.getFullName(), subject, html));
    }

    /**
     * Notifies the DPO inbox when a new data-rights request is submitted,
     * so processing can begin within the 30-day window.
     */
    private void sendDpoDataRightsNotification(AppUser user, DataRightsRequest req) {
        String subject = "[ACTION REQUIRED] New DPDP Rights Request #" + req.getId()
                + " — " + req.getRightType() + " — Due " + req.getDueBy().toLocalDate();
        String html = "<div style='font-family:sans-serif;max-width:600px;'>"
            + "<h2 style='color:#dc2626;'>New Data Rights Request — Action Required</h2>"
            + "<p>A data principal has submitted a rights request that must be processed within <strong>30 days</strong>.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>"
            + "<tr><td style='padding:8px;background:#fef2f2;font-weight:600;width:40%;'>Reference #</td>"
            + "<td style='padding:8px;'>" + req.getId() + "</td></tr>"
            + "<tr><td style='padding:8px;background:#fef2f2;font-weight:600;'>Right Type</td>"
            + "<td style='padding:8px;'><strong>" + req.getRightType() + "</strong></td></tr>"
            + "<tr><td style='padding:8px;background:#fef2f2;font-weight:600;'>Data Principal</td>"
            + "<td style='padding:8px;'>" + user.getFullName() + " &lt;" + user.getEmail() + "&gt;</td></tr>"
            + "<tr><td style='padding:8px;background:#fef2f2;font-weight:600;'>Submitted</td>"
            + "<td style='padding:8px;'>" + req.getCreatedAt().toLocalDate() + "</td></tr>"
            + "<tr><td style='padding:8px;background:#fef2f2;font-weight:600;'>Due By</td>"
            + "<td style='padding:8px;'><strong style='color:#dc2626;'>" + req.getDueBy().toLocalDate() + "</strong></td></tr>"
            + (req.getDescription() != null ? "<tr><td style='padding:8px;background:#fef2f2;font-weight:600;'>Details</td>"
            + "<td style='padding:8px;'>" + req.getDescription() + "</td></tr>" : "")
            + "</table>"
            + "<p>Process via the admin panel: <em>Privacy &gt; Data Rights Requests</em> (API: POST /api/privacy/admin/data-rights/" + req.getId() + "/process).</p>"
            + "<p style='color:#64748b;font-size:12px;'>This notification was generated automatically by Mana Community. [LEGAL REVIEW REQUIRED]</p>"
            + "</div>";
        emailService.send(new EmailMessage(dpoEmail, "Grievance Officer", subject, html));
    }
}
