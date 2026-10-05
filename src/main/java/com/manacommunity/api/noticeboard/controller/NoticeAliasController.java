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

    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(noticeService.getById(id, user));
    }
}
