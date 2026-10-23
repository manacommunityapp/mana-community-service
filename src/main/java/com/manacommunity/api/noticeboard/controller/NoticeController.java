package com.manacommunity.api.noticeboard.controller;

import com.manacommunity.api.noticeboard.dto.NoticeRequest;
import com.manacommunity.api.noticeboard.dto.NoticeResponse;
import com.manacommunity.api.noticeboard.dto.NoticeStatsResponse;
import com.manacommunity.api.noticeboard.service.NoticeService;
import com.manacommunity.api.user.model.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Tag(name = "Noticeboard", description = "Enterprise community announcements, broadcast alerts, and circulars APIs")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "Get active published notices targeted to current user")
    public ResponseEntity<List<NoticeResponse>> getActiveNotices(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.getActiveNotices(communityId, category, currentUser));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all notices in community (Admin view)")
    public ResponseEntity<List<NoticeResponse>> getAllNotices(
            @RequestParam Long communityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.getAllNotices(communityId, currentUser));
    }

    @GetMapping("/my")
    @Operation(summary = "Get notices authored by current user")
    public ResponseEntity<List<NoticeResponse>> getMyNotices(@AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.getMyNotices(currentUser.getId(), currentUser));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notice details by ID (automatically logs read receipt)")
    public ResponseEntity<NoticeResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.getById(id, currentUser));
    }

    @PostMapping
    @Operation(summary = "Publish or schedule a new notice with target audience segmentation")
    public ResponseEntity<NoticeResponse> create(
            @Valid @RequestBody NoticeRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return new ResponseEntity<>(noticeService.create(req, currentUser, currentUser.getCommunity()), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge receipt of a mandatory notice")
    public ResponseEntity<NoticeResponse> acknowledgeNotice(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.acknowledgeNotice(id, currentUser));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get notice readership and acknowledgement metrics")
    public ResponseEntity<NoticeStatsResponse> getNoticeStats(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.getNoticeStats(id, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notice details")
    public ResponseEntity<NoticeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.update(id, req, currentUser.getId(), currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notice")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        noticeService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pin")
    @Operation(summary = "Toggle pin state of a notice")
    public ResponseEntity<NoticeResponse> togglePin(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(noticeService.togglePin(id, currentUser));
    }
}
