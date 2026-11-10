package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyPostDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyPostService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitty-posts")
@RequiredArgsConstructor
public class KittyPartyPostController {

    private final KittyPartyPostService postService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/circles/{circleId}")
    public ResponseEntity<PostResponse> createPost(
            @PathVariable Long circleId,
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        PostResponse response = postService.createPost(user, circleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/circles/{circleId}")
    public ResponseEntity<List<PostResponse>> getPosts(
            @PathVariable Long circleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = null;
        if (principal != null) {
            try {
                currentUserId = loggedInUserService.resolve(principal).getId();
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(postService.getPostsByCircle(circleId, currentUserId));
    }

    @PostMapping("/{postId}/vote")
    public ResponseEntity<PostResponse> castVote(
            @PathVariable Long postId,
            @Valid @RequestBody CastVoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(postService.castVote(user, postId, request.getOptionId()));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPostDetail(postId));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        return ResponseEntity.ok(postService.updatePost(user, postId, request));
    }

    @PostMapping("/{postId}/pin")
    public ResponseEntity<PostResponse> togglePinPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(postService.togglePinPost(user, postId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        postService.deletePost(user, postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        CommentResponse response = postService.addComment(user, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getComments(postId));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        postService.deleteComment(user, commentId);
        return ResponseEntity.noContent().build();
    }
}
