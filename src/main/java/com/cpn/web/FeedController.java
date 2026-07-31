package com.cpn.web;

import com.cpn.application.feed.FeedService;
import com.cpn.domain.feed.model.Comment;
import com.cpn.domain.feed.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/posts")
    public ResponseEntity<List<Post>> getPosts(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(feedService.getAllPosts(type));
    }

    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        return ResponseEntity.ok(feedService.createPost(post));
    }

    @PostMapping("/posts/{postId}/react")
    public ResponseEntity<Post> reactPost(@PathVariable UUID postId, @RequestParam String type) {
        return ResponseEntity.ok(feedService.reactToPost(postId, type));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable UUID postId,
            @RequestParam UUID userId,
            @RequestParam String userName,
            @RequestBody String content) {
        return ResponseEntity.ok(feedService.addComment(postId, userId, userName, content));
    }
}
