package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventRegistrationReportRowDto;
import com.manacommunity.api.events.dto.EventReportResponse;
import com.manacommunity.api.events.service.EventReportService;
import com.manacommunity.api.events.sqs.ReportJobSqsHandler;
import com.manacommunity.api.events.sqs.ReportJobSqsHandler.ReportJobPayload;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.platform.sqs.SqsMessagePublisher;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventReportController {

    private final EventReportService reportService;

    @Autowired(required = false)
    private LoggedInUserService loggedInUserService;
    @Autowired(required = false)
    private SqsMessagePublisher sqsMessagePublisher;
    @Autowired(required = false)
    private SqsProperties sqsProperties;
    @Autowired(required = false)
    private ReportJobSqsHandler reportJobSqsHandler;

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<EventReportResponse> getEventReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getEventReport(id));
    }

    @GetMapping("/{id}/report/registrations")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventRegistrationReportRowDto>> getEventRegistrationsReport(
            @PathVariable Long id,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category) {
        return ResponseEntity.ok(reportService.getEventRegistrationsReport(id, category));
    }

    @GetMapping("/{id}/report/registrations/export")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<byte[]> exportRegistrationsCsv(
            @PathVariable Long id,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category) {
        byte[] csvBytes = reportService.generateRegistrationsCsv(id, category);
        String categoryName = (category != null && !category.isBlank()) ? category.toLowerCase() : "all";
        String filename = "event-" + id + "-registrations-" + categoryName + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    /**
     * Queues async CSV export generation via SQS ({@code mana-report-jobs}) and returns 202 with jobId.
     */
    @PostMapping("/{id}/report/registrations/export-async")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<Map<String, Object>> exportRegistrationsCsvAsync(
            @PathVariable Long id,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category,
            @AuthenticationPrincipal UserPrincipal principal) {
        String jobId = UUID.randomUUID().toString();
        Long requesterId = null;
        if (loggedInUserService != null && principal != null) {
            AppUser user = loggedInUserService.resolve(principal);
            requesterId = user != null ? user.getId() : null;
        }
        ReportJobPayload payload = new ReportJobPayload(jobId, id, category, requesterId);
        if (sqsMessagePublisher != null && sqsProperties != null) {
            sqsMessagePublisher.publish(
                    sqsProperties.getQueues().getReportJobs(),
                    "EVENT_REGISTRATIONS_CSV",
                    payload,
                    reportJobSqsHandler != null ? () -> reportJobSqsHandler.processReportJob(payload) : null
            );
        } else if (reportJobSqsHandler != null) {
            reportJobSqsHandler.processReportJob(payload);
        }
        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "jobId", jobId,
                "eventId", id
        ));
    }

    @GetMapping("/reports/jobs/{jobId}")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<byte[]> downloadAsyncReport(@PathVariable String jobId) {
        if (reportJobSqsHandler == null) {
            return ResponseEntity.notFound().build();
        }
        return reportJobSqsHandler.getCachedReport(jobId)
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .body(bytes))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}