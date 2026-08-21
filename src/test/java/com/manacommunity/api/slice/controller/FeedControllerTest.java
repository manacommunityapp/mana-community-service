package com.manacommunity.api.slice.controller;

import com.manacommunity.api.controller.FeedController;
import com.manacommunity.api.dto.PostRequest;
import com.manacommunity.api.dto.PostResponse;
import com.manacommunity.api.model.PostType;
import com.manacommunity.api.service.EngagementService;
import com.manacommunity.api.service.FeedService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@DisplayName("FeedController")
class FeedControllerTest extends BaseWebMvcTest {

    @MockitoBean LoggedInUserService loggedInUserService;
    @MockitoBean FeedService feedService;
    @MockitoBean EngagementService engagementService;

    private AppUser memberUser;
    private PostResponse postResponse;

    @BeforeEach
    void setUp() {
        memberUser = TestDataBuilder.memberUser();

        postResponse = new PostResponse(
                100L,
                "Community announcement #updates",
                "Announcement",
                null,
                false,
                false,
                5,
                2,
                0,
                0,
                10,
                false,
                false,
                null,
                null,
                1L,
                "Test User",
                null,
                "MEMBER",
                null,
                java.time.LocalDateTime.now(),
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
                false,
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
                "APPROVED"
        );
    }

    @Nested
    @DisplayName("GET /api/posts")
    class GetFeed {

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("returns 200 with paginated posts")
        void getFeed_returns200() throws Exception {
            Page<PostResponse> page = new PageImpl<>(List.of(postResponse));
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(feedService.getFeed(any(), any(), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100L))
                    .andExpect(jsonPath("$.content[0].content").value("Community announcement #updates"));
        }
    }

    @Nested
    @DisplayName("POST /api/posts")
    class CreatePost {

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("creates post and returns 200")
        void createPost_returns200() throws Exception {
            PostRequest req = new PostRequest(
                    "Community announcement #updates",
                    null, null, PostType.GENERAL, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(feedService.createPost(any(), any())).thenReturn(postResponse);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L));
        }
    }

    @Nested
    @DisplayName("Post & Comment Likes and Likers")
    class LikesAndLikersEndpoints {

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("GET /api/posts/{id}/likes returns 200 with list of likers")
        void getPostLikers_returns200() throws Exception {
            var liker = new com.manacommunity.api.dto.PostLikerResponse(
                    1L, "Test User", null, "Verified Member", "LIKE", java.time.LocalDateTime.now()
            );
            when(feedService.getPostLikers(100L)).thenReturn(List.of(liker));

            mockMvc.perform(get("/api/posts/100/likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(1L))
                    .andExpect(jsonPath("$[0].fullName").value("Test User"))
                    .andExpect(jsonPath("$[0].reactionType").value("LIKE"));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("POST /api/posts/comments/{commentId}/like returns 200 with updated toggle state")
        void toggleCommentLike_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(feedService.toggleCommentLike(any(), eq(200L)))
                    .thenReturn(new com.manacommunity.api.dto.CommentLikeToggleResponse(200L, 1, true));

            mockMvc.perform(post("/api/posts/comments/200/like"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.commentId").value(200L))
                    .andExpect(jsonPath("$.likesCount").value(1))
                    .andExpect(jsonPath("$.liked").value(true));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("GET /api/posts/comments/{commentId}/likes returns 200 with comment likers")
        void getCommentLikers_returns200() throws Exception {
            var liker = new com.manacommunity.api.dto.CommentLikerResponse(
                    1L, "Test User", null, "Verified Member", java.time.LocalDateTime.now()
            );
            when(feedService.getCommentLikers(200L)).thenReturn(List.of(liker));

            mockMvc.perform(get("/api/posts/comments/200/likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(1L))
                    .andExpect(jsonPath("$[0].fullName").value("Test User"));
        }
    }
}
