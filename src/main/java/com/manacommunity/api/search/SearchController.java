package com.manacommunity.api.search;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchIndexService searchIndexService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) List<String> modules,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int clampedSize = Math.min(size, 50);
        SearchResponse response = searchIndexService.search(
                principal.getCommunityId(), q.trim(), modules, page, clampedSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMMUNITY_ADMIN')")
    public ResponseEntity<Map<String, Object>> reindex(
            @AuthenticationPrincipal UserPrincipal principal) {

        int count = searchIndexService.reindexCommunity(principal.getCommunityId());
        return ResponseEntity.ok(Map.of("indexed", count, "communityId", principal.getCommunityId()));
    }

    @PostMapping("/index")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMMUNITY_ADMIN')")
    public ResponseEntity<Void> indexItem(
            @RequestBody SearchableItem item,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Enforce community isolation — ignore communityId in body
        SearchableItem scoped = new SearchableItem(
                principal.getCommunityId(),
                item.module(),
                item.entityId(),
                item.title(),
                item.subtitle(),
                item.description(),
                item.tags(),
                item.imageUrl(),
                item.deepLinkType(),
                item.deepLinkId(),
                item.visibility(),
                item.ownerUserId(),
                item.metadata()
        );
        searchIndexService.index(scoped);
        return ResponseEntity.accepted().build();
    }
}
