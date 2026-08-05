package com.manacommunity.api.noticeboard.controller;

import com.manacommunity.api.noticeboard.dto.NoticeRequest;
import com.manacommunity.api.noticeboard.dto.NoticeResponse;
import com.manacommunity.api.noticeboard.service.NoticeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Notices')")
    public ResponseEntity<List<NoticeResponse>> getNotices(
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(noticeService.getActiveNotices(communityId, category));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('Delete Notice')")
    public ResponseEntity<List<NoticeResponse>> getAllNotices(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(noticeService.getAllNotices(communityId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Notices')")
    public ResponseEntity<List<NoticeResponse>> getMyNotices(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.getMyNotices(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Notices')")
    public ResponseEntity<NoticeResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.getById(id, user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Notice')")
    public ResponseEntity<NoticeResponse> create(
            @Valid @RequestBody NoticeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noticeService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Notice')")
    public ResponseEntity<NoticeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.update(id, req, user.getId()));
    }

    @PutMapping("/{id}/pin")
    @PreAuthorize("hasAuthority('Create Notice')")
    public ResponseEntity<NoticeResponse> togglePin(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.togglePin(id, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Delete Notice')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        noticeService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
