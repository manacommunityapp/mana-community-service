package com.cpn.web;

import com.cpn.application.gamification.GamificationService;
import com.cpn.domain.gamification.model.GamificationBadge;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/badges/{userId}")
    public ResponseEntity<List<GamificationBadge>> getUserBadges(@PathVariable UUID userId) {
        return ResponseEntity.ok(gamificationService.getUserBadges(userId));
    }

    @PostMapping("/badges/award")
    public ResponseEntity<GamificationBadge> awardBadge(@RequestBody GamificationBadge badge) {
        return ResponseEntity.ok(gamificationService.awardBadge(badge));
    }
}
