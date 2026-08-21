package com.manacommunity.api.email;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin-only endpoint for querying the email delivery log and managing dispatches.
 *
 * <p>All operations require {@code ROLE_ADMIN}, {@code ROLE_SUPER_ADMIN}, or {@code ROLE_COMMUNITY_ADMIN}.
 * Results are paginated, newest first, and filterable by status, template type,
 * community, recipient email fragment / keyword, and date range.</p>
 */
@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COMMUNITY_ADMIN')")
public class EmailDeliveryLogController {

    private final EmailDeliveryLogRepository repo;
    private final EmailService emailService;

    /**
     * Paginated, filterable delivery log.
     */
    @GetMapping("/delivery-log")
    public ResponseEntity<Page<EmailDeliveryLogDto>> getDeliveryLog(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int safeSize = Math.min(size, 200);
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "sentAt"));

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        String effectiveKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : (recipient != null && !recipient.isBlank() ? recipient.trim() : null);

        Page<EmailDeliveryLogDto> result = repo
                .findFiltered(
                        status       != null && !status.equalsIgnoreCase("ALL") ? status.toUpperCase() : null,
                        templateType != null && !templateType.equalsIgnoreCase("ALL") ? templateType : null,
                        communityId,
                        effectiveKeyword,
                        fromDt,
                        toDt,
                        pageable)
                .map(EmailDeliveryLogDto::from);

        return ResponseEntity.ok(result);
    }

    /**
     * Summary counts for admin dashboard widgets with delivery rate, open rate, and category breakdown.
     */
    @GetMapping("/delivery-log/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long sent    = repo.countByCommunityIdAndStatusAndSentAtAfter(communityId, EmailDeliveryLog.STATUS_SENT,    since);
        long failed  = repo.countByCommunityIdAndStatusAndSentAtAfter(communityId, EmailDeliveryLog.STATUS_FAILED,  since);
        long skipped = repo.countByCommunityIdAndStatusAndSentAtAfter(communityId, EmailDeliveryLog.STATUS_SKIPPED, since);
        long opened  = repo.countByCommunityIdAndOpenedAtIsNotNullAndSentAtAfter(communityId, since);
        long total   = sent + failed + skipped;

        double deliveryRate = total > 0 ? ((double) sent / total) * 100.0 : 100.0;
        double openRate     = sent > 0 ? ((double) opened / sent) * 100.0 : 0.0;

        List<Object[]> typeDistribution = repo.countByTemplateTypeGrouped(communityId, since);
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (Object[] row : typeDistribution) {
            String type = row[0] != null ? (String) row[0] : "GENERAL";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            categoryCounts.put(type, count);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("communityId", communityId);
        resp.put("periodDays", days);
        resp.put("sent", sent);
        resp.put("failed", failed);
        resp.put("skipped", skipped);
        resp.put("opened", opened);
        resp.put("total", total);
        resp.put("deliveryRate", Math.round(deliveryRate * 10.0) / 10.0);
        resp.put("openRate", Math.round(openRate * 10.0) / 10.0);
        resp.put("categoryCounts", categoryCounts);

        return ResponseEntity.ok(resp);
    }

    /**
     * Get a single delivery log entry by id (with full HTML body for admin preview).
     */
    @GetMapping("/delivery-log/{id}")
    public ResponseEntity<EmailDeliveryLogDto> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(EmailDeliveryLogDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Resend an email from the delivery log (optionally to an override address).
     */
    @PostMapping("/delivery-log/{id}/resend")
    public ResponseEntity<Map<String, Object>> resendEmail(
            @PathVariable Long id,
            @RequestBody(required = false) ResendEmailRequest request) {
        EmailDeliveryLog existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmailDeliveryLog", id));

        String targetRecipient = (request != null && request.overrideRecipient() != null && !request.overrideRecipient().isBlank())
                ? request.overrideRecipient().trim()
                : existing.getRecipient();

        String body = existing.getBody();
        if (body == null || body.isBlank()) {
            body = "<p>" + (existing.getSubject() != null ? existing.getSubject() : "Notification") + "</p>";
        }

        EmailMessage msg = EmailMessage.builder()
                .to(targetRecipient)
                .subject(existing.getSubject() != null ? existing.getSubject() : "Community Notification")
                .htmlBody(body)
                .build();

        if (existing.getTemplateType() != null) {
            org.slf4j.MDC.put(SmtpEmailService.MDC_TEMPLATE_TYPE, existing.getTemplateType());
        }
        if (existing.getCommunityId() != null) {
            org.slf4j.MDC.put(SmtpEmailService.MDC_COMMUNITY_ID, String.valueOf(existing.getCommunityId()));
        }

        try {
            emailService.send(msg);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Email re-dispatched to " + targetRecipient,
                    "recipient", targetRecipient
            ));
        } finally {
            org.slf4j.MDC.remove(SmtpEmailService.MDC_TEMPLATE_TYPE);
            org.slf4j.MDC.remove(SmtpEmailService.MDC_COMMUNITY_ID);
        }
    }

    /**
     * Send an on-demand custom notification/test email directly from the admin hub.
     */
    @PostMapping("/send-custom")
    public ResponseEntity<Map<String, Object>> sendCustomEmail(
            @RequestBody @Valid SendCustomEmailRequest request) {
        if (request.to() == null || request.to().isBlank()) {
            throw new InvalidInputException("Recipient email address is required");
        }
        if (request.subject() == null || request.subject().isBlank()) {
            throw new InvalidInputException("Subject is required");
        }
        if (request.body() == null || request.body().isBlank()) {
            throw new InvalidInputException("Email body message is required");
        }

        String templateType = request.templateType() != null && !request.templateType().isBlank()
                ? request.templateType().trim()
                : "CUSTOM_COMMUNICATION";

        if (templateType != null) {
            org.slf4j.MDC.put(SmtpEmailService.MDC_TEMPLATE_TYPE, templateType);
        }
        if (request.communityId() != null) {
            org.slf4j.MDC.put(SmtpEmailService.MDC_COMMUNITY_ID, String.valueOf(request.communityId()));
        }

        EmailMessage msg = EmailMessage.builder()
                .to(request.to().trim())
                .subject(request.subject().trim())
                .htmlBody(request.body())
                .build();

        try {
            emailService.send(msg);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Custom email sent successfully to " + request.to(),
                    "recipient", request.to()
            ));
        } finally {
            org.slf4j.MDC.remove(SmtpEmailService.MDC_TEMPLATE_TYPE);
            org.slf4j.MDC.remove(SmtpEmailService.MDC_COMMUNITY_ID);
        }
    }

    /**
     * List all registered email templates (from the EmailTemplate enum).
     */
    @GetMapping("/system-templates")
    public ResponseEntity<Map<String, Object>> getTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long communityId) {

        List<EmailTemplateInfo> templates = Arrays.stream(EmailTemplate.values())
                .filter(t -> category == null || t.category().name().equalsIgnoreCase(category))
                .map(t -> new EmailTemplateInfo(
                        t.name(),
                        t.templateName(),
                        t.defaultSubject(),
                        t.category().name(),
                        t.trigger().menuPath(),
                        t.trigger().wired(),
                        t.trigger().description(),
                        false, null, null, null,
                        "DEFAULT"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("count", templates.size(), "templates", templates));
    }

    public record ResendEmailRequest(String overrideRecipient) {}

    public record SendCustomEmailRequest(
            @NotBlank String to,
            @NotBlank String subject,
            @NotBlank String body,
            String templateType,
            Long communityId
    ) {}

    public record EmailTemplateInfo(
            String key,
            String templateFile,
            String subject,
            String category,
            String triggerMenuPath,
            boolean triggerWired,
            String triggerDescription,
            boolean customTemplateExists,
            Long customTemplateId,
            String customTemplateName,
            String customTemplateStatus,
            String appliedSource
    ) {}
}
