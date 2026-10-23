package com.manacommunity.api.groupbuy.controller;

import com.manacommunity.api.groupbuy.dto.DemandItemRequest;
import com.manacommunity.api.groupbuy.dto.DemandItemResponse;
import com.manacommunity.api.groupbuy.service.GroupDemandService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/group-buying/demand")
@RequiredArgsConstructor
public class GroupDemandController {

    private final GroupDemandService demandService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DemandItemResponse>> getBoard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        return ResponseEntity.ok(demandService.getBoard(communityId, page, size, user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandItemResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(demandService.getById(id, user.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandItemResponse> create(
            @Valid @RequestBody DemandItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandService.create(req, user, user.getCommunity()));
    }

    @PostMapping("/{id}/upvote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandItemResponse> upvote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(demandService.upvote(id, user.getId()));
    }
}
