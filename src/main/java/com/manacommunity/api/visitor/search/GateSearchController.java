package com.manacommunity.api.visitor.search;

import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gate/search")
@RequiredArgsConstructor
public class GateSearchController {

    private final GateFuzzySearchService fuzzySearchService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<List<GateFuzzySearchService.GateSearchHit>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "communityId", required = false) Long communityId,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long resolvedCommunityId = communityId;
        if (resolvedCommunityId == null && principal != null) {
            AppUser user = loggedInUserService.resolve(principal);
            if (user != null && user.getCommunity() != null) {
                resolvedCommunityId = user.getCommunity().getId();
            }
        }
        return ResponseEntity.ok(fuzzySearchService.searchTypeahead(resolvedCommunityId, query, limit));
    }

    @GetMapping("/es-mapping")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<Map<String, Object>> elasticsearchMapping() {
        return ResponseEntity.ok(fuzzySearchService.getElasticsearchMappingSpec());
    }
}
