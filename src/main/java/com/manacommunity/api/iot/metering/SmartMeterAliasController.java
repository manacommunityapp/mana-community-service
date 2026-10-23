package com.manacommunity.api.iot.metering;

import com.manacommunity.api.model.SmartMeter;
import com.manacommunity.api.service.SmartMeterService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/api/v1/iot/meters", "/iot/meters"})
@RequiredArgsConstructor
public class SmartMeterAliasController {

    private final SmartMeterService smartMeterService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<?> getMetersForUnit(
            @PathVariable String unitId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        List<SmartMeter> allMeters = smartMeterService.getMeters(communityId, unitId, null);
        return ResponseEntity.ok(allMeters);
    }

    @GetMapping("/summary/{unitId}")
    public ResponseEntity<?> getConsumptionSummary(
            @PathVariable String unitId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        List<SmartMeter> meters = smartMeterService.getMeters(communityId, unitId, null);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("unitId", unitId);
        summary.put("communityId", communityId);

        List<Map<String, Object>> meterSummaries = meters.stream().map(m -> {
            Map<String, Object> ms = new LinkedHashMap<>();
            ms.put("meterId", m.getId());
            ms.put("meterType", m.getMeterType());
            ms.put("lastReading", m.getLastReading());
            ms.put("lastReadingDate", m.getLastReadingDate());
            ms.put("active", m.getActive());
            return ms;
        }).collect(Collectors.toList());

        summary.put("meters", meterSummaries);
        summary.put("totalMeters", meters.size());

        Map<String, Object> usage = smartMeterService.getCommunityUsage(communityId, null, LocalDate.now().getMonthValue(), null);
        summary.put("communityUsage", usage);

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{meterId}/pulse")
    public ResponseEntity<?> simulatePulse(
            @PathVariable Long meterId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        SmartMeter meter = smartMeterService.getMeter(communityId, meterId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meterId", meterId);
        result.put("status", "pulse_received");
        result.put("lastReading", meter.getLastReading());
        result.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }
}
