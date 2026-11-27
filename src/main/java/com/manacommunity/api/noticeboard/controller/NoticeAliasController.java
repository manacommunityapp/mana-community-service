package com.manacommunity.api.noticeboard.controller;

import com.manacommunity.api.noticeboard.dto.NoticeResponse;
import com.manacommunity.api.noticeboard.service.NoticeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Alias controller so the mobile app can call /api/notices without a communityId query param.
 * Infers community from the authenticated user.
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeAliasController {

    private final NoticeService noticeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices(
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(noticeService.getActiveNotices(communityId, category, user));
    }

    @GetMapping("/all")
    public ResponseEntity<List<NoticeResponse>> getAllNotices(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(noticeService.getAllNotices(communityId, user));
    }

    @GetMapping({"/mine", "/my"})
    public ResponseEntity<List<NoticeResponse>> getMyNotices(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.getMyNotices(user.getId(), user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.getById(id, user));
    }

    @PostMapping
    public ResponseEntity<NoticeResponse> create(
            @jakarta.validation.Valid @RequestBody com.manacommunity.api.noticeboard.dto.NoticeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return new org.springframework.http.ResponseEntity<>(
                noticeService.create(req, user, user.getCommunity()),
                org.springframework.http.HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoticeResponse> update(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody com.manacommunity.api.noticeboard.dto.NoticeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.update(id, req, user.getId(), user));
    }

    @RequestMapping(value = "/{id}/pin", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<NoticeResponse> togglePin(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.togglePin(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        noticeService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
