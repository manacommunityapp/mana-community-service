package com.manacommunity.api.slice.controller;

import com.manacommunity.api.events.controller.EventVolunteerController;
import com.manacommunity.api.events.dto.EventVolunteerRequest;
import com.manacommunity.api.events.dto.EventVolunteerResponse;
import com.manacommunity.api.events.service.EventVolunteerService;
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
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventVolunteerController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@DisplayName("EventVolunteerController")
class EventVolunteerControllerTest extends BaseWebMvcTest {

    @MockitoBean EventVolunteerService volunteerService;
    @MockitoBean LoggedInUserService loggedInUserService;

    private AppUser adminUser;
    private EventVolunteerResponse sampleResponse;

    @BeforeEach
    void setUp() {
        adminUser = TestDataBuilder.adminUser();

        sampleResponse = EventVolunteerResponse.builder()
                .id(1L)
                .eventId(100L)
                .userId(2L)
                .userName("Volunteer One")
                .role("Coordinator")
                .status("CONFIRMED")
                .build();
    }

    @Nested
    @DisplayName("GET /api/events/volunteers")
    class GetVolunteers {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("returns list of volunteers by eventId")
        void getByEvent_returns200() throws Exception {
            when(volunteerService.getByEvent(100L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events/volunteers").param("eventId", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].role").value("Coordinator"));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("returns list of volunteers by community")
        void getByCommunity_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(volunteerService.getByCommunity(1L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events/volunteers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L));
        }
    }

    @Nested
    @DisplayName("POST & PUT /api/events/volunteers")
    class ModifyVolunteers {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("POST creates volunteer and returns 201")
        void create_returns201() throws Exception {
            EventVolunteerRequest req = new EventVolunteerRequest();
            req.setEventId(100L);
            req.setUserId(2L);
            req.setRole("Coordinator");

            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(volunteerService.create(any(), any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/events/volunteers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("PUT /{id}/check-in marks volunteer as checked in")
        void checkIn_returns200() throws Exception {
            sampleResponse.setCheckInTime("10:00 AM");
            when(volunteerService.checkIn(1L)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/events/volunteers/1/check-in"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checkInTime").value("10:00 AM"));
        }
    }
}
