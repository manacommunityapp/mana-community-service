package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.FeedService;
import com.manacommunity.api.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FeedController {

    private final LoggedInUserService loggedInUserService;
    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        Page<PostResponse> response = feedService.getFeed(currentUser, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PostRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        PostResponse response = feedService.createPost(currentUser, request);
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

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        LikeToggleResponse response = feedService.toggleLike(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id) {
        List<CommentResponse> response = feedService.getComments(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody CommentRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        CommentResponse response = feedService.addComment(currentUser, id, request);
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
}
