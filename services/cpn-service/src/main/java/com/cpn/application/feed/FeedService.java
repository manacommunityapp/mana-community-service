package com.cpn.application.feed;

import com.cpn.domain.feed.model.Comment;
import com.cpn.domain.feed.model.Post;
import com.cpn.domain.feed.repository.CommentRepository;
import com.cpn.domain.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<Post> getAllPosts(String postType) {
        if (postType != null && !postType.isEmpty() && !"all".equalsIgnoreCase(postType)) {
            return postRepository.findByPostTypeOrderByCreatedAtDesc(postType.toUpperCase());
        }
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Post createPost(Post post) {
        if (post.getPostType() == null) post.setPostType("GENERAL");
        return postRepository.save(post);
    }

    @Transactional
    public Post reactToPost(UUID postId, String reactionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if ("like".equalsIgnoreCase(reactionType)) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else if ("celebrate".equalsIgnoreCase(reactionType)) {
            post.setCelebrateCount(post.getCelebrateCount() + 1);
        } else if ("insightful".equalsIgnoreCase(reactionType)) {
            post.setInsightfulCount(post.getInsightfulCount() + 1);
        }
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(UUID postId, UUID userId, String userName, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        Comment c = Comment.builder()
                .post(post)
                .userId(userId)
                .userName(userName)
                .content(content)
                .build();
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        return commentRepository.save(c);
    }
}
