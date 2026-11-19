package com.manacommunity.api.communityops.watchlist;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService service;

    @PostMapping
    public ResponseEntity<WatchlistEntry> add(@RequestBody WatchlistEntryDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.add(principal.getCommunityId(), principal.getUserId(), req));
    }

    @GetMapping
    public ResponseEntity<List<WatchlistEntry>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getAll(principal.getCommunityId()));
    }

    @PostMapping("/check")
    public ResponseEntity<List<WatchlistEntry>> check(@RequestBody CheckRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.check(principal.getCommunityId(), req.identifier()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.deactivate(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    record CheckRequest(String identifier) {}
}
