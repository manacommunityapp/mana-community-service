package com.manacommunity.api.slice.controller;

import com.manacommunity.api.events.controller.EventDonationController;
import com.manacommunity.api.events.dto.EventDonationRequest;
import com.manacommunity.api.events.dto.EventDonationResponse;
import com.manacommunity.api.events.service.EventDonationService;
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

@WebMvcTest(EventDonationController.class)
@DisplayName("EventDonationController")
class EventDonationControllerTest extends BaseWebMvcTest {

    @MockitoBean EventDonationService donationService;
    @MockitoBean LoggedInUserService loggedInUserService;

    private AppUser adminUser;
    private EventDonationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        adminUser = TestDataBuilder.adminUser();

        sampleResponse = EventDonationResponse.builder()
                .id(1L)
                .eventId(100L)
                .donorName("John Doe")
                .amount(1000.0)
                .build();
    }

    @Nested
    @DisplayName("GET /api/events/donations")
    class GetDonations {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("returns list of donations by eventId")
        void getByEvent_returns200() throws Exception {
            when(donationService.getByEvent(100L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events/donations").param("eventId", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].donorName").value("John Doe"))
                    .andExpect(jsonPath("$[0].amount").value(1000.0));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Events"})
        @DisplayName("returns list of donations by community")
        void getByCommunity_returns200() throws Exception {
            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(donationService.getByCommunity(1L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/events/donations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L));
        }
    }

    @Nested
    @DisplayName("POST /api/events/donations")
    class CreateDonation {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Event"})
        @DisplayName("creates donation and returns 201")
        void create_returns201() throws Exception {
            EventDonationRequest req = new EventDonationRequest();
            req.setEventId(100L);
            req.setDonorName("John Doe");
            req.setAmount(1000.0);

            when(loggedInUserService.resolve(any())).thenReturn(adminUser);
            when(donationService.create(any(), any(), any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/events/donations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L));
        }
    }
}
