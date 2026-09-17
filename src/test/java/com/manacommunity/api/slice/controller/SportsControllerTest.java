package com.manacommunity.api.slice.controller;

import com.manacommunity.api.controller.SportsController;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.repository.SportsPlayerCategoryRepository;
import com.manacommunity.api.repository.SportsMetaRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsEventCsvImportService;
import com.manacommunity.api.service.SportsEventService;
import com.manacommunity.api.service.SportsTournamentService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.WithMockUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_MAIN;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_MENU;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
