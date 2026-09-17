package com.manacommunity.api.slice.controller;

import com.manacommunity.api.controller.SportsAnalyticsController;
import com.manacommunity.api.dto.dashboard.SportsAnalyticsResponse;
import com.manacommunity.api.dto.dashboard.SportsAnalyticsResponse.*;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsAnalyticsService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_ANALYTICS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SportsAnalyticsController.class)
@DisplayName("SportsAnalyticsController")
class SportsAnalyticsControllerTest extends BaseWebMvcTest {

    @MockitoBean SportsAnalyticsService analyticsService;
    @MockitoBean LoggedInUserService loggedInUserService;
    @MockitoBean PermissionCheckService permissionCheckService;

    private SportsAnalyticsResponse createMockResponse() {
        OverviewMetrics overview = new OverviewMetrics(5, 2, 10, 4, 50, 40, 10, 35, 3);
        SportParticipationMetric sportMetric = new SportParticipationMetric(1L, "Badminton", "🏸", 4, 30, 25);
        RegistrationStatusMetric statusMetric = new RegistrationStatusMetric("CONFIRMED", 40L, 80.0);
        GenderBreakdown genderBreakdown = new GenderBreakdown(25, 15, 0, 62.5, 37.5);
        AgeGroupBreakdown ageBreakdown = new AgeGroupBreakdown(10, 15, 15, 0);
        TournamentMetric tournamentMetric = new TournamentMetric(1L, "Summer Cup", "LIVE", 32, 28, 25, 78.1);
        VenueUtilizationMetric venueMetric = new VenueUtilizationMetric(1L, "Main Court", 6L, 2L);
        AuctionAnalytics auctionSummary = new AuctionAnalytics(1, 4, 24, 100000L, 85000L);

        return new SportsAnalyticsResponse(
                overview,
                List.of(sportMetric),
                List.of(statusMetric),
                genderBreakdown,
                ageBreakdown,
                List.of(tournamentMetric),
                List.of(venueMetric),
                auctionSummary
        );
    }

    @Nested
    @DisplayName("GET /api/sports/analytics")
    class GetAnalytics {

        @Test
        @WithMockUserPrincipal(role = "SPORTS_ADMIN")
        @DisplayName("sports admin with VIEW_SPORTS_ANALYTICS gets 200 with complete analytics")
        void sportsAdmin_authorized_returns200() throws Exception {
            SportsAnalyticsResponse response = createMockResponse();
            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(VIEW_SPORTS_ANALYTICS));
            when(analyticsService.getAnalytics(any(), any())).thenReturn(response);

            mockMvc.perform(get("/api/sports/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overview.totalTournaments").value(5))
                    .andExpect(jsonPath("$.overview.totalRegistrations").value(50))
                    .andExpect(jsonPath("$.sportParticipation[0].sportName").value("Badminton"))
                    .andExpect(jsonPath("$.auctionSummary.totalAuctionTeams").value(4));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("unauthorized role without permission receives 403 Forbidden")
        void unauthorizedMember_returns403() throws Exception {
            doThrow(new AccessDeniedException("Access Denied"))
                    .when(permissionCheckService).requireAnyPermission(any(), eq(VIEW_SPORTS_ANALYTICS));

            mockMvc.perform(get("/api/sports/analytics"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/sports/analytics/overview")
    class GetOverview {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("admin receives 200 with lean overview metrics")
        void admin_authorized_returnsOverview() throws Exception {
            SportsAnalyticsResponse response = createMockResponse();
            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(VIEW_SPORTS_ANALYTICS));
            when(analyticsService.getAnalytics(any(), any())).thenReturn(response);

            mockMvc.perform(get("/api/sports/analytics/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTournaments").value(5))
                    .andExpect(jsonPath("$.activeEvents").value(4))
                    .andExpect(jsonPath("$.uniqueParticipants").value(35));
        }
    }
}
