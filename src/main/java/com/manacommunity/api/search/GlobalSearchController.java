package com.manacommunity.api.search;

import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    /**
     * GET /api/search?q={query}&limit={limit}
     * Cross-module search scoped to the caller's community.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<GlobalSearchResult>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = principal.getCommunityId();
        List<GlobalSearchResult> results = searchService.search(q, communityId, limit);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
