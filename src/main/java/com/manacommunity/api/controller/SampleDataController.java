package com.manacommunity.api.controller;

import com.manacommunity.api.service.sample.SampleDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
public class SampleDataController {

    private final SampleDataService sampleDataService;

    /**
     * POST /api/admin/seed/auction
     * Executes the sample_data.sql file to populate the database.
     * Note: Depending on your security config, you may want to restrict this
     * to SUPER_ADMIN or allow it openly for dev environments.
     */
    @PostMapping("/auction")
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Uncomment in production
    public ResponseEntity<String> seedAuctionData() {
        String resultMessage = sampleDataService.executeSampleDataSql();
        if (resultMessage.startsWith("Error") || resultMessage.startsWith("Failed")) {
            return ResponseEntity.badRequest().body(resultMessage);
        }
        return ResponseEntity.ok(resultMessage);
    }
}
