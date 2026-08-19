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
}
