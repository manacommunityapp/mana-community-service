package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.PostRequest;
import com.manacommunity.api.dto.PostResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Post;
import com.manacommunity.api.model.PostType;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.FeedService;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService")
class FeedServiceTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostCommentRepository postCommentRepository;
    @Mock PostPollVoteRepository pollVoteRepository;
    @Mock PostReactionRepository postReactionRepository;
    @Mock PostBookmarkRepository postBookmarkRepository;
    @Mock PostMediaRepository postMediaRepository;
    @Mock PostHashtagRepository postHashtagRepository;
    @Mock HashtagRepository hashtagRepository;
    @Mock CommunityLeaderRepository communityLeaderRepository;

    @InjectMocks FeedService feedService;

    private Community community;
    private AppUser user;
    private Post post;

    @BeforeEach
    void setUp() {
        community = TestDataBuilder.community(1L, "INVITE123");
        user = TestDataBuilder.memberUser();
        user.setCommunity(community);

        post = Post.builder()
                .id(100L)
                .content("Welcome to our community feed! #general")
                .postType(PostType.GENERAL)
                .user(user)
                .community(community)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Query Feed")
    class QueryFeed {

        @Test
        @DisplayName("getFeed returns paginated posts for user's community")
        void getFeed_success() {
            Page<Post> page = new PageImpl<>(List.of(post));
            when(postRepository.findByCommunityIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(page);

            Page<PostResponse> result = feedService.getFeed(user, "ALL", 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).content()).contains("Welcome to our community feed!");
        }

        @Test
        @DisplayName("getFeed throws InvalidInputException if user has no community")
        void getFeed_noCommunity() {
            user.setCommunity(null);

            assertThatThrownBy(() -> feedService.getFeed(user, "ALL", 0, 10))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("User is not associated with any community");
        }
    }

    @Nested
    @DisplayName("Create & Delete Post")
    class ModifyPost {

        @Test
        @DisplayName("createPost creates new post with hashtags")
        void createPost_success() {
            PostRequest req = new PostRequest(
                    "Hello Mana Community! #welcome",
                    "Welcome",
                    null,
                    PostType.GENERAL,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setId(101L);
                return p;
            });

            PostResponse response = feedService.createPost(user, req);

            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("deletePost soft-deletes post when caller is author")
        void deletePost_success() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            feedService.deletePost(user, 100L);

            assertThat(post.isDeleted()).isTrue();
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("deletePost throws ResourceNotFoundException if post missing")
        void deletePost_notFound() {
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedService.deletePost(user, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
