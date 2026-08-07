package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventReportResponse;
import com.manacommunity.api.events.service.EventReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
