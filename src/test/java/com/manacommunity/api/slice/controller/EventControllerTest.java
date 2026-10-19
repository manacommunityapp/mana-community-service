package com.manacommunity.api.slice.controller;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.events.controller.EventController;
import com.manacommunity.api.events.dto.*;
import com.manacommunity.api.events.service.EventService;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@DisplayName("EventController")
class EventControllerTest extends BaseWebMvcTest {

    @MockitoBean EventService eventService;
    @MockitoBean LoggedInUserService loggedInUserService;

    private AppUser adminUser;
    private AppUser memberUser;
    private EventResponse sampleResponse;

    @BeforeEach
    void setUp() {
        adminUser = TestDataBuilder.adminUser();
        memberUser = TestDataBuilder.memberUser();

        sampleResponse = EventResponse.builder()
                .id(100L)
                .title("Annual Sports Meet")
                .type("SPORTS")
                .status("PUBLISHED")
                .startDate("2026-09-01")
                .capacity(100)
                .attendees(10)
                .isRegistered(false)
                .build();
    }

    // ── GET /api/events ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/events")
    class GetEvents {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("returns 200 with list of upcoming events")
        void getUpcoming_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.getUpcomingEvents(any(), any(), any())).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100L))
                    .andExpect(jsonPath("$[0].title").value("Annual Sports Meet"));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("GET /api/events/all returns 200 with all events")
        void getAll_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.getAllEvents(any(), any())).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100L));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"View Events", "Register Event"})
        @DisplayName("GET /api/events/mine returns 200 with user registered events")
        void getMine_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(eventService.getMyEvents(eq(memberUser.getId()))).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100L));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("GET /api/events/{id} returns 200 with event detail")
        void getById_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.getById(eq(100L), any())).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/events/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.title").value("Annual Sports Meet"));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("GET /api/events/{id} returns 404 when not found")
        void getById_returns404() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.getById(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Event", 999L));

            mockMvc.perform(get("/api/events/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/events & PUT /api/events/{id} ───────────────────────

    @Nested
    @DisplayName("Create & Update Event")
    class CreateUpdate {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("POST /api/events creates event and returns 201")
        void create_returns201() throws Exception {
            EventRequest req = TestDataBuilder.eventRequest("New Event");
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.create(any(), any(), any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100L));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("POST /api/events returns 400 for missing start date")
        void create_returns400_missingStartDate() throws Exception {
            EventRequest req = TestDataBuilder.eventRequest("New Event");
            req.setStartDate(null);
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.create(any(), any(), any()))
                    .thenThrow(new ManaCommunityException("Event start date is required",
                            HttpStatus.BAD_REQUEST, "MISSING_EVENT_DATE"));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("PUT /api/events/{id} updates event and returns 200")
        void update_returns200() throws Exception {
            EventRequest req = TestDataBuilder.eventRequest("Updated Title");
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.update(eq(100L), any(), any())).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/events/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("PUT /api/events/{id} returns 409 for cancelled event")
        void update_returns409_cancelledEvent() throws Exception {
            EventRequest req = TestDataBuilder.eventRequest("Change Title");
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.update(eq(100L), any(), any()))
                    .thenThrow(new ManaCommunityException("Cannot modify a cancelled event",
                            HttpStatus.CONFLICT, "EVENT_CANCELLED"));

            mockMvc.perform(put("/api/events/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event", "Delete Event Schedule"})
        @DisplayName("DELETE /api/events/{id} deletes event and returns 204")
        void delete_returns204() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);

            mockMvc.perform(delete("/api/events/100"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event", "Delete Event Schedule"})
        @DisplayName("DELETE /api/events/{id} returns 409 when event has registrations")
        void delete_returns409_hasRegistrations() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            doThrow(new ManaCommunityException("Cannot delete event with 3 registration(s)",
                    HttpStatus.CONFLICT, "EVENT_HAS_REGISTRATIONS"))
                    .when(eventService).delete(eq(100L), any());

            mockMvc.perform(delete("/api/events/100"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"View Events"})
        @DisplayName("DELETE /api/events/{id} returns 403 for unauthorized user")
        void delete_returns403_unauthorized() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            doThrow(new UnauthorizedActionException("Only the event creator can delete this event"))
                    .when(eventService).delete(eq(100L), any());

            mockMvc.perform(delete("/api/events/100"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Register & Unregister ─────────────────────────────────────────

    @Nested
    @DisplayName("Registration endpoints")
    class RegistrationEndpoints {

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"Register Event", "View Events"})
        @DisplayName("POST /api/events/{id}/register returns 200 with updated event")
        void register_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(eventService.register(eq(100L), any(), any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/events/100/register"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"Register Event", "View Events"})
        @DisplayName("POST /api/events/{id}/register returns 409 for capacity full")
        void register_returns409_capacityFull() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(eventService.register(eq(100L), any()))
                    .thenThrow(new EventFullException("Annual Sports Meet", 100));

            mockMvc.perform(post("/api/events/100/register"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"Register Event", "View Events"})
        @DisplayName("POST /api/events/{id}/register returns 409 for already registered")
        void register_returns409_alreadyRegistered() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(eventService.register(eq(100L), any()))
                    .thenThrow(new AlreadyRegisteredException("Annual Sports Meet"));

            mockMvc.perform(post("/api/events/100/register"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"Register Event", "View Events"})
        @DisplayName("POST /api/events/{id}/register returns 409 for deadline passed")
        void register_returns409_deadlinePassed() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(eventService.register(eq(100L), any()))
                    .thenThrow(new ManaCommunityException("Registration deadline has passed",
                            HttpStatus.CONFLICT, "REGISTRATION_DEADLINE_PASSED"));

            mockMvc.perform(post("/api/events/100/register"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"Register Event", "View Events"})
        @DisplayName("DELETE /api/events/{id}/register returns 200 with updated event")
        void unregister_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(memberUser);
            when(eventService.unregister(eq(100L), any())).thenReturn(sampleResponse);

            mockMvc.perform(delete("/api/events/100/register"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L));
        }
    }

    // ── Registration Management ──────────────────────────────────────

    @Nested
    @DisplayName("Registration Management endpoints")
    class RegistrationManagement {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events", "Manage Registrations"})
        @DisplayName("GET /api/events/{id}/registrations returns registration list")
        void getRegistrations_returns200() throws Exception {
            RegistrationResponse reg = RegistrationResponse.builder()
                    .id(1L).eventId(100L).userId(2L).userName("Test User")
                    .status("CONFIRMED").checkedIn(false).build();
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.getEventRegistrations(100L)).thenReturn(List.of(reg));

            mockMvc.perform(get("/api/events/100/registrations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                    .andExpect(jsonPath("$[0].checkedIn").value(false));
        }
    }

    // ── Dashboard Stats ───────────────────────────────────────────────

    @Nested
    @DisplayName("Dashboard Stats")
    class DashboardEndpoints {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events", "View Event Dashboard"})
        @DisplayName("GET /api/events/dashboard/stats returns 200 with stats")
        void dashboardStats_returns200() throws Exception {
            DashboardStatsResponse stats = DashboardStatsResponse.builder()
                    .totalEvents(5)
                    .totalRegistrations(50)
                    .totalVolunteers(10)
                    .build();

            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(eventService.getDashboardStats(any())).thenReturn(stats);

            mockMvc.perform(get("/api/events/dashboard/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEvents").value(5))
                    .andExpect(jsonPath("$.totalRegistrations").value(50));
        }
    }
}
