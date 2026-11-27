package com.manacommunity.api.dailyhelp.ranking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/dailyhelp/recommendations")
@RequiredArgsConstructor
public class DomesticHelpRankingController {

    private final DomesticHelpRankingService rankingService;

    /**
     * Search and rank domestic helpers (cooks, maids, nannies, plumbers).
     * Combines hyperlocal proximity, gate attendance reliability, neighbor endorsements,
     * and Thompson Sampling multi-armed bandit cold-start fairness.
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DomesticHelpRecommendationResponse> searchDomesticHelp(
            @Valid @RequestBody DomesticHelpRankingRequest request) {
        log.info("Searching domestic helpers for communityId={} category={} tower={}",
                request.communityId(), request.category(), request.residentTower());
        return ResponseEntity.ok(rankingService.rankDomesticHelp(request));
    }

    /**
     * GET endpoint for fast feed carousel loading.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DomesticHelpRecommendationResponse> getRecommendations(
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "COOK") String category,
            @RequestParam(required = false, defaultValue = "") String tower,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        DomesticHelpRankingRequest request = DomesticHelpRankingRequest.builder()
                .communityId(communityId)
                .category(category)
                .residentTower(tower)
                .limit(limit)
                .build();
        return ResponseEntity.ok(rankingService.rankDomesticHelp(request));
    }
}
