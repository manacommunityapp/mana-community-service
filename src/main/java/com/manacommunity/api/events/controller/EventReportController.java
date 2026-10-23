package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventRegistrationReportRowDto;
import com.manacommunity.api.events.dto.EventReportResponse;
import com.manacommunity.api.events.service.EventReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventReportController {

    private final EventReportService reportService;

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
}