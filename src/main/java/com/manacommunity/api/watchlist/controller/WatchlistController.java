package com.manacommunity.api.watchlist.controller;

import com.manacommunity.api.watchlist.dto.WatchlistRequest;
import com.manacommunity.api.watchlist.entity.WatchlistEntry;
import com.manacommunity.api.watchlist.service.WatchlistService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<WatchlistEntry>> getWatchlist(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(watchlistService.getWatchlist(loggedInUserService.resolve(principal)));
    }

    @PostMapping
    public ResponseEntity<WatchlistEntry> add(@RequestBody WatchlistRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchlistService.add(req, loggedInUserService.resolve(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WatchlistEntry> update(@PathVariable Long id,
                                                  @RequestBody WatchlistRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(watchlistService.update(id, req, loggedInUserService.resolve(principal)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WatchlistEntry> updateStatus(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(watchlistService.updateStatus(id, body.get("status"), loggedInUserService.resolve(principal)));
    }
}
