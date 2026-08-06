package com.cpos.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/cpos/occupancy")
@RequiredArgsConstructor
@CrossOrigin
public class OccupancyController {

    @GetMapping("/community/{communityId}/summary")
    public ResponseEntity<Map<String, Object>> getOccupancySummary(@PathVariable UUID communityId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("communityId", communityId);
        summary.put("totalUnits", 2847);
        summary.put("ownerOccupied", 1240);
        summary.put("tenantOccupied", 1180);
        summary.put("vacant", 322);
        summary.put("reserved", 105);
        summary.put("occupancyRatePct", 84.9);
        return ResponseEntity.ok(summary);
    }
}
