package com.manacommunity.api.forum;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Community forum API — threaded discussion boards for residents.
 * All endpoints require authentication only; no special roles required.
 */
@RestController
@RequestMapping("/api/forum")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ForumController {

    private final ForumPostRepository postRepo;
    private final ForumCommentRepository commentRepo;
    private final LoggedInUserService loggedInUserService;

    // ── Posts ──────────────────────────────────────────────────────────────────

    /** GET /api/forum/posts — list posts with optional category / search filter */
    @GetMapping("/posts")
    public ResponseEntity<Page<Map<String, Object>>> getPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = resolveCommunityId(user);
        if (communityId == null) return ResponseEntity.ok(Page.empty());

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumPost> posts;
        if (search != null && !search.isBlank()) {
            posts = postRepo.searchByCommunity(communityId, search, pageable);
        } else if (category != null && !category.isBlank()) {
            posts = postRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category, pageable);
        } else {
            posts = postRepo.findByCommunityIdOrderByPinnedDescCreatedAtDesc(communityId, pageable);
        }
        return ResponseEntity.ok(posts.map(p -> toPostSummary(p, user)));
    }

    /** GET /api/forum/posts/{id} — single post with comments */
    @GetMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return postRepo.findById(id)
                .map(p -> ResponseEntity.ok(toPostDetail(p, user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/forum/posts — create a new post */
    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Community community = user.getCommunity();
        if (community == null) return ResponseEntity.badRequest().build();

        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String content = String.valueOf(body.getOrDefault("content", "")).trim();
        if (title.isEmpty() || content.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String category = body.get("category") != null ? String.valueOf(body.get("category")) : null;

        ForumPost post = ForumPost.builder()
                .title(title.length() > 200 ? title.substring(0, 200) : title)
                .content(content)
                .category(category)
                .author(user)
                .community(community)
                .build();
        post = postRepo.save(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPostSummary(post, user));
    }

    /** PUT /api/forum/posts/{id} — edit own post */
    @PutMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        return postRepo.findById(id).map(post -> {
            if (!post.getAuthor().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Map<String, Object>>build();
            }
            if (body.get("title") != null) {
                String t = String.valueOf(body.get("title")).trim();
                post.setTitle(t.length() > 200 ? t.substring(0, 200) : t);
            }
            if (body.get("content") != null) post.setContent(String.valueOf(body.get("content")));
            if (body.get("category") != null) post.setCategory(String.valueOf(body.get("category")));
            return ResponseEntity.ok(toPostSummary(postRepo.save(post), user));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/forum/posts/{id} — delete own post */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return postRepo.findById(id).map(post -> {
            if (!post.getAuthor().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
            }
            postRepo.delete(post);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Comments ───────────────────────────────────────────────────────────────

    /** POST /api/forum/posts/{postId}/comments — add a comment */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        return postRepo.findById(postId).map(post -> {
            String content = String.valueOf(body.getOrDefault("content", "")).trim();
            if (content.isEmpty()) return ResponseEntity.badRequest().<Map<String, Object>>build();

            ForumComment comment = ForumComment.builder()
                    .content(content)
                    .author(user)
                    .post(post)
                    .build();
            comment = commentRepo.save(comment);

            post.setCommentCount(post.getCommentCount() + 1);
            postRepo.save(post);

            return ResponseEntity.status(HttpStatus.CREATED).body(toCommentMap(comment));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/forum/posts/{postId}/comments — list all comments for a post */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<Map<String, Object>>> getComments(
            @PathVariable Long postId) {
        if (!postRepo.existsById(postId)) return ResponseEntity.notFound().build();
        List<Map<String, Object>> comments = commentRepo.findByPostIdOrderByCreatedAtAsc(postId)
                .stream().map(this::toCommentMap).toList();
        return ResponseEntity.ok(comments);
    }

    /** DELETE /api/forum/comments/{id} — delete own comment */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return commentRepo.findById(id).map(comment -> {
            if (!comment.getAuthor().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
            }
            ForumPost post = comment.getPost();
            commentRepo.delete(comment);
            if (post != null) {
                post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
                postRepo.save(post);
            }
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Long resolveCommunityId(AppUser user) {
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }

    private Map<String, Object> toPostSummary(ForumPost p, AppUser viewer) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           p.getId());
        m.put("title",        p.getTitle());
        m.put("category",     p.getCategory());
        m.put("authorId",     p.getAuthor().getId());
        m.put("authorName",   p.getAuthor().getFullName());
        m.put("commentCount", p.getCommentCount());
        m.put("likeCount",    p.getLikeCount());
        m.put("isPinned",     p.isPinned());
        m.put("isClosed",     p.isClosed());
        m.put("isOwn",        viewer != null && viewer.getId().equals(p.getAuthor().getId()));
        m.put("createdAt",    p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("updatedAt",    p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        // Include truncated preview of content
        String preview = p.getContent();
        m.put("preview",      preview.length() > 200 ? preview.substring(0, 200) + "…" : preview);
        return m;
    }

    private Map<String, Object> toPostDetail(ForumPost p, AppUser viewer) {
        Map<String, Object> m = toPostSummary(p, viewer);
        m.put("content", p.getContent());
        List<Map<String, Object>> comments = commentRepo.findByPostIdOrderByCreatedAtAsc(p.getId())
                .stream().map(c -> toCommentMap(c)).toList();
        m.put("comments", comments);
        return m;
    }

    private Map<String, Object> toCommentMap(ForumComment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         c.getId());
        m.put("content",    c.getContent());
        m.put("authorId",   c.getAuthor().getId());
        m.put("authorName", c.getAuthor().getFullName());
        m.put("likeCount",  c.getLikeCount());
        m.put("createdAt",  c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return m;
    }
}
