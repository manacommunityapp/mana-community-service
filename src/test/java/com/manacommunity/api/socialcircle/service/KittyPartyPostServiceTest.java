package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.socialcircle.dto.KittyPartyPostDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyPost;
import com.manacommunity.api.socialcircle.entity.KittyPartyPostComment;
import com.manacommunity.api.socialcircle.repository.KittyPartyCircleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyPostCommentRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyPostRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kitty Party Post Service Test Suite")
class KittyPartyPostServiceTest {

    @Mock
    private KittyPartyPostRepository postRepository;
    @Mock
    private KittyPartyPostCommentRepository commentRepository;
    @Mock
    private KittyPartyCircleRepository circleRepository;

    @InjectMocks
    private KittyPartyPostService postService;

    private AppUser author;
    private AppUser commenter;
    private KittyPartyCircle circle;
    private KittyPartyPost post;
    private KittyPartyPostComment comment;

    @BeforeEach
    void setUp() {
        Community community = new Community();
        community.setId(1L);

        author = new AppUser();
        author.setId(10L);
        author.setFullName("Sunita Rao");
        author.setCommunity(community);

        commenter = new AppUser();
        commenter.setId(20L);
        commenter.setFullName("Pooja Nair");
        commenter.setCommunity(community);

        circle = KittyPartyCircle.builder()
                .id(100L)
                .circleName("Mana Kitty Queens")
                .community(community)
                .status("ACTIVE")
                .build();

        post = KittyPartyPost.builder()
                .id(600L)
                .circle(circle)
                .author(author)
                .postType("ANNOUNCEMENT")
                .title("Theme for December Kitty: Royal Velvet")
                .content("Hi everyone! The dress code for our December gathering is Royal Velvet. Host: Sunita.")
                .isPinned(false)
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .build();

        comment = KittyPartyPostComment.builder()
                .id(700L)
                .post(post)
                .user(commenter)
                .commentText("Excited! Can't wait.")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create post successfully")
    void testCreatePostSuccess() {
        when(circleRepository.findById(100L)).thenReturn(Optional.of(circle));
        when(postRepository.save(any(KittyPartyPost.class))).thenAnswer(inv -> {
            KittyPartyPost p = inv.getArgument(0);
            p.setId(600L);
            return p;
        });

        CreatePostRequest req = CreatePostRequest.builder()
                .postType("ANNOUNCEMENT")
                .title("Theme for December Kitty: Royal Velvet")
                .content("Hi everyone! The dress code for our December gathering is Royal Velvet.")
                .isPinned(true)
                .build();

        PostResponse resp = postService.createPost(author, 100L, req);

        assertNotNull(resp);
        assertEquals(600L, resp.getId());
        assertEquals("Theme for December Kitty: Royal Velvet", resp.getTitle());
        assertTrue(resp.isPinned());
        assertEquals("PINNED", resp.getStatus());
        verify(postRepository).save(any(KittyPartyPost.class));
    }

    @Test
    @DisplayName("Should list posts ordered by pinned status and creation date")
    void testGetPostsByCircle() {
        when(postRepository.findByCircleIdOrderByPinnedAndCreatedAt(100L)).thenReturn(List.of(post));
        when(commentRepository.countByPostId(600L)).thenReturn(1L);

        List<PostResponse> posts = postService.getPostsByCircle(100L);

        assertEquals(1, posts.size());
        assertEquals(600L, posts.get(0).getId());
        assertEquals(1L, posts.get(0).getCommentsCount());
    }

    @Test
    @DisplayName("Should toggle pin status of post")
    void testTogglePinPost() {
        when(postRepository.findById(600L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(KittyPartyPost.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse resp = postService.togglePinPost(author, 600L);

        assertTrue(resp.isPinned());
        assertEquals("PINNED", resp.getStatus());
    }

    @Test
    @DisplayName("Should update post content")
    void testUpdatePost() {
        when(postRepository.findById(600L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(KittyPartyPost.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePostRequest req = UpdatePostRequest.builder()
                .title("Updated Theme: Emerald Elegance")
                .content("Dress code changed to Emerald Green.")
                .build();

        PostResponse resp = postService.updatePost(author, 600L, req);

        assertEquals("Updated Theme: Emerald Elegance", resp.getTitle());
        assertEquals("Dress code changed to Emerald Green.", resp.getContent());
    }

    @Test
    @DisplayName("Should soft delete post")
    void testDeletePost() {
        when(postRepository.findById(600L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(KittyPartyPost.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.deletePost(author, 600L);

        assertEquals("DELETED", post.getStatus());
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("Should add comment to post successfully")
    void testAddComment() {
        when(postRepository.findById(600L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(KittyPartyPostComment.class))).thenAnswer(inv -> {
            KittyPartyPostComment c = inv.getArgument(0);
            c.setId(700L);
            return c;
        });

        CreateCommentRequest req = CreateCommentRequest.builder()
                .commentText("Excited! Can't wait.")
                .build();

        CommentResponse resp = postService.addComment(commenter, 600L, req);

        assertNotNull(resp);
        assertEquals(700L, resp.getId());
        assertEquals("Excited! Can't wait.", resp.getCommentText());
        assertEquals("Pooja Nair", resp.getUserName());
    }

    @Test
    @DisplayName("Should get comments for a post")
    void testGetComments() {
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(600L)).thenReturn(List.of(comment));

        List<CommentResponse> comments = postService.getComments(600L);

        assertEquals(1, comments.size());
        assertEquals("Pooja Nair", comments.get(0).getUserName());
    }

    @Test
    @DisplayName("Should delete comment successfully")
    void testDeleteComment() {
        when(commentRepository.findById(700L)).thenReturn(Optional.of(comment));

        postService.deleteComment(commenter, 700L);

        verify(commentRepository).delete(comment);
    }
}
