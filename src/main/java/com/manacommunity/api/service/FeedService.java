package com.manacommunity.api.service;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(AppUser currentUser, int page, int size) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByCommunityIdOrderByCreatedAtDesc(currentUser.getCommunity().getId(), pageable);
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
    }

    @Transactional
    public PostResponse createPost(AppUser currentUser, PostRequest request) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        boolean isOfficial = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        Post post = Post.builder()
                .user(currentUser)
                .community(currentUser.getCommunity())
                .content(request.content())
                .imageUrl(request.imageUrl())
                .official(isOfficial)
                .likesCount(0)
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);
        return toPostResponse(savedPost, currentUser.getId());
    }

    @Transactional
    public void deletePost(AppUser currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        boolean isAdmin = isAdminRole(currentUser.getRole());
        if (!post.getUser().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new UnauthorizedActionException("delete post " + postId);
        }
        postRepository.delete(post);
    }

    @Transactional
    public LikeToggleResponse toggleLike(AppUser currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, currentUser.getId());
        boolean liked;
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            liked = false;
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(currentUser)
                    .build();
            postLikeRepository.save(newLike);
            post.setLikesCount(post.getLikesCount() + 1);
            liked = true;
        }
        postRepository.save(post);
        return new LikeToggleResponse(post.getLikesCount(), liked);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        // Just verify post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", postId);
        }
        List<PostComment> comments = postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream().map(this::toCommentResponse).toList();
    }

    @Transactional
    public CommentResponse addComment(AppUser currentUser, Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        PostComment comment = PostComment.builder()
                .post(post)
                .user(currentUser)
                .content(request.content())
                .build();

        PostComment savedComment = postCommentRepository.save(comment);
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return toCommentResponse(savedComment);
    }

    @Transactional
    public void deleteComment(AppUser currentUser, Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        boolean isAdmin = isAdminRole(currentUser.getRole());
        boolean isCommentAuthor = comment.getUser().getId().equals(currentUser.getId());
        boolean isPostAuthor = comment.getPost().getUser().getId().equals(currentUser.getId());

        if (!isCommentAuthor && !isPostAuthor && !isAdmin) {
            throw new UnauthorizedActionException("delete comment " + commentId);
        }

        Post post = comment.getPost();
        postCommentRepository.delete(comment);
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
    }

    private PostResponse toPostResponse(Post post, Long currentUserId) {
        AppUser author = post.getUser();
        String initials = getInitials(author.getFullName());
        boolean liked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getImageUrl(),
                post.isOfficial(),
                post.getLikesCount(),
                post.getCommentsCount(),
                liked,
                author.getId(),
                author.getFullName(),
                initials,
                mapRole(author.getRole()),
                post.getCreatedAt()
        );
    }

    private CommentResponse toCommentResponse(PostComment comment) {
        AppUser author = comment.getUser();
        String initials = getInitials(author.getFullName());

        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                author.getId(),
                author.getFullName(),
                initials,
                mapRole(author.getRole()),
                comment.getCreatedAt()
        );
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN");

    private boolean isAdminRole(String role) {
        return role != null && ADMIN_ROLES.contains(role.toUpperCase());
    }

    private String mapRole(String rawRole) {
        if (isAdminRole(rawRole)) return "Admin";
        return "Verified Member";
    }
}
