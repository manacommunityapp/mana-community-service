package com.manacommunity.api.slice.controller;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.sports.controller.SportsController;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsMeta;
import com.manacommunity.api.sports.repository.SportsPlayerCategoryRepository;
import com.manacommunity.api.sports.repository.SportsMetaRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.sports.service.SportsEventCsvImportService;
import com.manacommunity.api.sports.service.SportsEventService;
import com.manacommunity.api.sports.service.SportsTournamentService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.WithMockUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_SPORTS_MAIN;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_MAIN;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_MENU;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SportsController.class)
@DisplayName("SportsController")
class SportsControllerTest extends BaseWebMvcTest {

    @MockitoBean SportsEventService         eventService;
    @MockitoBean SportsMetaRepository        sportMetaRepo;
    @MockitoBean SportsPlayerCategoryRepository   categoryRepo;
    @MockitoBean LoggedInUserService        loggedInUserService;
    @MockitoBean SportsTournamentService          tournamentService;
    @MockitoBean PermissionCheckService     permissionCheckService;
    @MockitoBean SportsEventCsvImportService csvImportService;
    @MockitoBean com.manacommunity.api.sports.repository.SportsEventRegistrationRepository registrationRepo;

    // ── GET /api/sports/meta ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sports/meta")
    class GetMeta {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("authorized user gets 200 with sport list")
        void authorized_returns200() throws Exception {
            SportsMeta badminton = new SportsMeta();
            badminton.setId(1L);
            badminton.setName("Badminton");

            AppUser mockUser = AppUser.builder().id(1L).build();
            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(VIEW_SPORTS_MAIN), eq(VIEW_SPORTS_MENU));
            when(loggedInUserService.resolveContext(any()))
                    .thenReturn(new LoggedInUserService.ResolvedUser(mockUser, true, 100L));
            when(sportMetaRepo.findByActiveTrue()).thenReturn(List.of(badminton));

            mockMvc.perform(get("/api/sports/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Badminton"));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("missing permission returns 403")
        void forbidden_returns403() throws Exception {
            doThrow(new AccessDeniedException("Insufficient permissions"))
                    .when(permissionCheckService).requireAnyPermission(any(), eq(VIEW_SPORTS_MAIN), eq(VIEW_SPORTS_MENU));

            mockMvc.perform(get("/api/sports/meta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated request returns 401 or 403")
        void unauthenticated_returns4xx() throws Exception {
            mockMvc.perform(get("/api/sports/meta"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ── GET /api/sports/events/{id} ───────────────────────────────────

    @Nested
    @DisplayName("GET /api/sports/events/{id}")
    class GetEventById {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("returns 200 when event exists")
        void eventExists_returns200() throws Exception {
            SportsEvent event = SportsEvent.builder()
                    .id(10L)
                    .name("Badminton Singles")
                    .build();

            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(VIEW_SPORTS_MAIN));
            when(eventService.getEventById(10L)).thenReturn(event);

            mockMvc.perform(get("/api/sports/events/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Badminton Singles"));
        }
    }

    // ── POST /api/sports/events ───────────────────────────────────────

    @Nested
    @DisplayName("POST /api/sports/events")
    class CreateSportsEvent {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("successfully creates event when format is provided as a JSON array (e.g. [\"TEAM\"])")
        void createEvent_withArrayFormat_returns201() throws Exception {
            SportsEvent created = SportsEvent.builder()
                    .id(50L)
                    .name("Inter-Community Cricket")
                    .build();

            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(CREATE_EDIT_SPORTS_MAIN));
            AppUser mockUser = AppUser.builder().id(1L).build();
            when(loggedInUserService.resolveContext(any()))
                    .thenReturn(new LoggedInUserService.ResolvedUser(mockUser, true, 100L));
            when(eventService.createEvent(any(), any())).thenReturn(created);

            String requestBody = """
                {
                    "name": "Inter-Community Cricket",
                    "sportId": 1,
                    "communityId": 100,
                    "eventDateStart": "2026-10-01",
                    "eventDateEnd": "2026-10-07",
                    "format": ["TEAM"]
                }
                """;

            mockMvc.perform(post("/api/sports/events")
                            .with(csrf())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(50))
                    .andExpect(jsonPath("$.name").value("Inter-Community Cricket"));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("successfully creates event when format is provided as legacy string (e.g. \"SINGLES\")")
        void createEvent_withStringFormat_returns201() throws Exception {
            SportsEvent created = SportsEvent.builder()
                    .id(51L)
                    .name("Open Badminton Singles")
                    .build();

            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(CREATE_EDIT_SPORTS_MAIN));
            AppUser mockUser = AppUser.builder().id(1L).build();
            when(loggedInUserService.resolveContext(any()))
                    .thenReturn(new LoggedInUserService.ResolvedUser(mockUser, true, 100L));
            when(eventService.createEvent(any(), any())).thenReturn(created);

            String requestBody = """
                {
                    "name": "Open Badminton Singles",
                    "sportId": 2,
                    "communityId": 100,
                    "eventDateStart": "2026-10-01",
                    "eventDateEnd": "2026-10-07",
                    "format": "SINGLES"
                }
                """;

            mockMvc.perform(post("/api/sports/events")
                            .with(csrf())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(51))
                    .andExpect(jsonPath("$.name").value("Open Badminton Singles"));
        }
    }

    // ── PUT /api/sports/events/{id} ───────────────────────────────────

    @Nested
    @DisplayName("PUT /api/sports/events/{id}")
    class UpdateSportsEvent {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("successfully updates event when format is array (e.g. [\"SINGLES\"])")
        void updateEvent_withArrayFormat_returns200() throws Exception {
            SportsEvent updated = SportsEvent.builder()
                    .id(2L)
                    .name("Updated Badminton Singles")
                    .build();

            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(CREATE_EDIT_SPORTS_MAIN));
            AppUser mockUser = AppUser.builder().id(1L).build();
            when(loggedInUserService.resolveContext(any()))
                    .thenReturn(new LoggedInUserService.ResolvedUser(mockUser, true, 100L));
            when(eventService.updateEvent(eq(2L), any())).thenReturn(updated);

            String requestBody = """
                {
                    "name": "Updated Badminton Singles",
                    "sportId": 1,
                    "communityId": 100,
                    "eventDateStart": "2026-10-01",
                    "eventDateEnd": "2026-10-07",
                    "format": ["SINGLES"]
                }
                """;

            mockMvc.perform(put("/api/sports/events/2")
                            .with(csrf())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("Updated Badminton Singles"));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("successfully updates event when format is array of multiple formats")
        void updateEvent_withMultipleFormats_returns200() throws Exception {
            SportsEvent updated = SportsEvent.builder()
                    .id(2L)
                    .name("Updated Badminton Championship")
                    .build();

            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(CREATE_EDIT_SPORTS_MAIN));
            AppUser mockUser = AppUser.builder().id(1L).build();
            when(loggedInUserService.resolveContext(any()))
                    .thenReturn(new LoggedInUserService.ResolvedUser(mockUser, true, 100L));
            when(eventService.updateEvent(eq(2L), any())).thenReturn(updated);

            String requestBody = """
                {
                    "name": "Updated Badminton Championship",
                    "sportId": 1,
                    "communityId": 100,
                    "eventDateStart": "2026-10-01",
                    "eventDateEnd": "2026-10-07",
                    "format": ["SINGLES", "DOUBLES"]
                }
                """;

            mockMvc.perform(put("/api/sports/events/2")
                            .with(csrf())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("Updated Badminton Championship"));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("successfully updates event when format is legacy string")
        void updateEvent_withStringFormat_returns200() throws Exception {
            SportsEvent updated = SportsEvent.builder()
                    .id(2L)
                    .name("Updated Badminton Singles String")
                    .build();

            doNothing().when(permissionCheckService).requireAnyPermission(any(), eq(CREATE_EDIT_SPORTS_MAIN));
            AppUser mockUser = AppUser.builder().id(1L).build();
            when(loggedInUserService.resolveContext(any()))
                    .thenReturn(new LoggedInUserService.ResolvedUser(mockUser, true, 100L));
            when(eventService.updateEvent(eq(2L), any())).thenReturn(updated);

            String requestBody = """
                {
                    "name": "Updated Badminton Singles String",
                    "sportId": 1,
                    "communityId": 100,
                    "eventDateStart": "2026-10-01",
                    "eventDateEnd": "2026-10-07",
                    "format": "SINGLES"
                }
                """;

            mockMvc.perform(put("/api/sports/events/2")
                            .with(csrf())
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("Updated Badminton Singles String"));
        }
    }
}
