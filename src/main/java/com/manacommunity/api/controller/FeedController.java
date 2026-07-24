package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.model.ReactionType;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.FeedService;
import com.manacommunity.api.service.EngagementService;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class FeedController {

    private final LoggedInUserService loggedInUserService;
    private final FeedService feedService;
    private final EngagementService engagementService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<PostResponse> response = feedService.getFeed(currentUser, type, Math.max(page, 0), safeSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Page<PostResponse>> getGroupFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<PostResponse> response = feedService.getGroupFeed(currentUser, groupId, Math.max(page, 0), safeSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        Page<PostResponse> response = feedService.searchPosts(currentUser, q, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<Page<PostResponse>> getBookmarks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        Page<PostResponse> response = feedService.getBookmarkedPosts(currentUser, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PostRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        PostResponse response = feedService.createPost(currentUser, request);
        engagementService.recordActivity(currentUser, "POST", 10);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        feedService.deletePost(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<PostResponse> pinPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        PostResponse response = feedService.pinPost(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/react")
    public ResponseEntity<ReactionResponse> toggleReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReactionRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        ReactionResponse response = feedService.toggleReaction(currentUser, id, request.reactionType());
        engagementService.recordActivity(currentUser, "REACTION_GIVEN", 2);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        LikeToggleResponse response = feedService.toggleLike(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<PostResponse> toggleBookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        PostResponse response = feedService.toggleBookmark(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        loggedInUserService.resolve(principal);
        List<CommentResponse> response = feedService.getComments(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        CommentResponse response = feedService.addComment(currentUser, id, request);
        engagementService.recordActivity(currentUser, "COMMENT", 5);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        feedService.deleteComment(currentUser, commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments/{commentId}/pin")
    public ResponseEntity<CommentResponse> pinComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        CommentResponse response = feedService.pinComment(currentUser, commentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comments/{commentId}/accept")
    public ResponseEntity<CommentResponse> acceptAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        CommentResponse response = feedService.markAcceptedAnswer(currentUser, commentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<PostResponse> voteOnPoll(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam String option) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        PostResponse response = feedService.voteOnPoll(currentUser, id, option);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<Void> reportPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        engagementService.reportContent(currentUser, "POST", id, request.reason(), request.description());
        return ResponseEntity.ok().build();
    }
}
