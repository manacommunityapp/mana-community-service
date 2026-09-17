package com.manacommunity.api.controller;

import com.manacommunity.api.service.sample.SampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Data Seeder / Feed", description = "Endpoints to seed, feed, and manage tournament, sports, and system sample data")
@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
public class SampleDataController {

    private final SampleDataService sampleDataService;

    @Operation(
            summary = "Feed Sports Data",
            description = "Feeds the 'LE 2026 Season Fest' tournament with all 6 sports sub-events (Cricket, Badminton, Chess, Carroms, Table Tennis, Volleyball), player categories, venues, and cricket auction configuration. Optionally seeds sample participant registrations for all events."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sports data successfully seeded"),
            @ApiResponse(responseCode = "400", description = "Failed to feed sports data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role")
    })
    @PostMapping("/sports-data")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> feedSportsData(
            @Parameter(description = "If true, seeds sample participant registrations across Badminton, Table Tennis, Chess, Carroms, and Volleyball in addition to Cricket. Default is false (clean registration rosters).")
            @RequestParam(defaultValue = "false") boolean includeSampleRegistrations) {
        Map<String, Object> result = sampleDataService.feedSportsData(includeSampleRegistrations);
        if ("ERROR".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Feed Sports Registrations Only",
            description = "Seeds sample participant registrations across Badminton (16 players), Table Tennis (8 players), Chess (8 players), Carroms (8 players), and Volleyball (12 players) into existing sub-events."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registrations seeded successfully"),
            @ApiResponse(responseCode = "400", description = "Failed to seed registrations")
    })
    @PostMapping("/sports-registrations")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> feedSportsRegistrations() {
        Map<String, Object> result = sampleDataService.feedSportsRegistrations();
        if ("ERROR".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Feed All Sample Data",
            description = "Seeds full end-to-end sample data across all modules (Users, Roles, Venues, Tournaments, Events, Auctions, and Inventories)."
    )
    @PostMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> seedAllData() {
        String message = sampleDataService.executeSampleDataSql();
        return ResponseEntity.ok(Map.of("message", message, "status", "SUCCESS"));
    }

    @Operation(
            summary = "Feed Default Base Data",
            description = "Seeds system baseline data (Communities, Roles, Base Player Categories, and Email Templates)."
    )
    @PostMapping("/default")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> seedDefaultData() {
        String message = sampleDataService.executeDefaultDataSql();
        return ResponseEntity.ok(Map.of("message", message, "status", "SUCCESS"));
    }

    @Operation(
            summary = "Seed Auction Data (Legacy)",
            description = "Executes the sample data seeder (legacy endpoint maintained for backward compatibility)."
    )
    @PostMapping("/auction")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> seedAuctionData() {
        String resultMessage = sampleDataService.executeSampleDataSql();
        if (resultMessage.startsWith("Error") || resultMessage.startsWith("Failed")) {
            return ResponseEntity.badRequest().body(resultMessage);
        }
        return ResponseEntity.ok(resultMessage);
    }
}
