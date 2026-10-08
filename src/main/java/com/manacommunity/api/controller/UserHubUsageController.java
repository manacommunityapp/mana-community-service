package com.manacommunity.api.controller;

import com.manacommunity.api.service.UserHubUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hub-usage")
@RequiredArgsConstructor
public class UserHubUsageController {

    private final UserHubUsageService hubUsageService;

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackClick(@RequestBody Map<String, String> request) {
        Long userId = Long.parseLong(request.get("userId"));
        String hubId = request.get("hubId");
        String hubLabel = request.get("hubLabel");

        hubUsageService.trackClick(userId, hubId, hubLabel);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/top/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTopHubs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(hubUsageService.getTopHubs(userId, limit));
    }
}
