package com.manacommunity.api.sports.controller;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.model.SportsAuctionConfig;
import com.manacommunity.api.sports.model.SportsAuctionWatch;
import com.manacommunity.api.sports.repository.SportsAuctionConfigRepository;
import com.manacommunity.api.sports.repository.SportsAuctionWatchRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auction/watch")
@RequiredArgsConstructor
public class SportsAuctionWatchController {

    private final SportsAuctionWatchRepository watchRepository;
    private final SportsAuctionConfigRepository configRepository;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/auction/watch — list all watched auctions for the current user. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getWatchList(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SportsAuctionWatch> watches = watchRepository.findByUserId(principal.getId());
        List<Map<String, Object>> result = watches.stream().map(w -> Map.<String, Object>of(
                "id", w.getId(),
                "configId", w.getConfig().getId(),
                "seasonName", w.getConfig().getSeasonName() != null ? w.getConfig().getSeasonName() : "",
                "status", w.getConfig().getStatus() != null ? w.getConfig().getStatus().name() : "",
                "createdAt", w.getCreatedAt().toString()
        )).toList();
        return ResponseEntity.ok(result);
    }

    /** POST /api/auction/watch/{configId} — add auction to watch list. */
    @PostMapping("/{configId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> watchAuction(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (watchRepository.existsByUserIdAndConfigId(principal.getId(), configId)) {
            return ResponseEntity.ok(Map.of("message", "Already watching", "configId", configId));
        }
        SportsAuctionConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionConfig", configId));
        AppUser user = loggedInUserService.resolve(principal);
        SportsAuctionWatch watch = SportsAuctionWatch.builder()
                .user(user)
                .config(config)
                .build();
        watchRepository.save(watch);
        log.info("User {} watching auction config {}", principal.getId(), configId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Added to watch list", "configId", configId));
    }

    /** DELETE /api/auction/watch/{configId} — remove auction from watch list. */
    @DeleteMapping("/{configId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> unwatchAuction(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        watchRepository.deleteByUserIdAndConfigId(principal.getId(), configId);
        log.info("User {} unwatched auction config {}", principal.getId(), configId);
        return ResponseEntity.ok(Map.of("message", "Removed from watch list", "configId", configId));
    }

    /** GET /api/auction/watch/{configId}/status — check if user is watching. */
    @GetMapping("/{configId}/status")
    public ResponseEntity<Map<String, Object>> isWatching(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean watching = watchRepository.existsByUserIdAndConfigId(principal.getId(), configId);
        long totalWatchers = watchRepository.countByConfigId(configId);
        return ResponseEntity.ok(Map.of("watching", watching, "watcherCount", totalWatchers));
    }
}
