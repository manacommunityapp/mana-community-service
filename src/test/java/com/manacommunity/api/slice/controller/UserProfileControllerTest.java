package com.manacommunity.api.slice.controller;

import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.user.controller.UserProfileController;
import com.manacommunity.api.user.dto.UserProfileResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserProfileController - Profile & Stats Slice Tests")
class UserProfileControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private LoggedInUserService loggedInUserService;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    @DisplayName("GET /api/profile returns 200 with UserProfileResponse")
    void getProfile_returns200() throws Exception {
        AppUser user = AppUser.builder().id(1L).fullName("Test User").email("test@example.com").build();
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .fullName("Test User")
                .email("test@example.com")
                .stats(UserProfileResponse.UserStats.builder()
                        .posts(5)
                        .eventsAttended(2)
                        .sportsPlayed(1)
                        .build())
                .build();

        when(loggedInUserService.resolve(any())).thenReturn(user);
        when(userProfileService.getProfile(any())).thenReturn(response);

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.stats.posts").value(5))
                .andExpect(jsonPath("$.stats.eventsAttended").value(2));
    }

    @Test
    @DisplayName("GET /api/profile/stats returns 200 with UserStats")
    void getProfileStats_returns200() throws Exception {
        AppUser user = AppUser.builder().id(1L).fullName("Test User").email("test@example.com").build();
        UserProfileResponse.UserStats stats = UserProfileResponse.UserStats.builder()
                .posts(10)
                .connections(42)
                .eventsAttended(3)
                .itemsSold(1)
                .jobsPosted(0)
                .sportsPlayed(2)
                .build();

        when(loggedInUserService.resolve(any())).thenReturn(user);
        when(userProfileService.getProfileStats(any())).thenReturn(stats);

        mockMvc.perform(get("/api/profile/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").value(10))
                .andExpect(jsonPath("$.connections").value(42))
                .andExpect(jsonPath("$.eventsAttended").value(3))
                .andExpect(jsonPath("$.itemsSold").value(1))
                .andExpect(jsonPath("$.sportsPlayed").value(2));
    }

    @Test
    @DisplayName("GET /api/profile/activities returns 200 with list of UserActivityItem")
    void getProfileActivities_returns200() throws Exception {
        AppUser user = AppUser.builder().id(1L).fullName("Test User").email("test@example.com").build();
        java.util.List<UserProfileResponse.UserActivityItem> activities = java.util.List.of(
                UserProfileResponse.UserActivityItem.builder()
                        .id(101L)
                        .type("post")
                        .text("Posted in Community Feed: 'Test Post'")
                        .time("2 hours ago")
                        .iconType("message")
                        .color("indigo")
                        .build(),
                UserProfileResponse.UserActivityItem.builder()
                        .id(102L)
                        .type("event")
                        .text("Registered for 'Annual Sports Day'")
                        .time("1 day ago")
                        .iconType("trophy")
                        .color("yellow")
                        .build()
        );

        when(loggedInUserService.resolve(any())).thenReturn(user);
        when(userProfileService.getUserActivities(any())).thenReturn(activities);

        mockMvc.perform(get("/api/profile/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].type").value("post"))
                .andExpect(jsonPath("$[0].text").value("Posted in Community Feed: 'Test Post'"))
                .andExpect(jsonPath("$[1].id").value(102))
                .andExpect(jsonPath("$[1].type").value("event"));
    }
}