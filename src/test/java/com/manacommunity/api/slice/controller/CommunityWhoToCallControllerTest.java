package com.manacommunity.api.slice.controller;

import com.manacommunity.api.controller.CommunityWhoToCallController;
import com.manacommunity.api.dto.CommunityWhoToCallHistoryResponse;
import com.manacommunity.api.dto.CommunityWhoToCallRequest;
import com.manacommunity.api.dto.CommunityWhoToCallResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.service.CommunityWhoToCallService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommunityWhoToCallController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommunityWhoToCallController - Slice Tests")
class CommunityWhoToCallControllerTest extends BaseWebMvcTest {

    @MockitoBean private CommunityWhoToCallService whoToCallService;
    @MockitoBean private LoggedInUserService loggedInUserService;

    @Nested
    @DisplayName("GET /api/community/who-to-call - Active contacts")
    class GetActive {

        @Test
        @DisplayName("returns 200 with list of active Who to Call contacts")
        void returns200() throws Exception {
            Community community = Community.builder().id(1L).name("Test Community").build();
            AppUser user = AppUser.builder().id(10L).community(community).build();
            when(loggedInUserService.resolve(any())).thenReturn(user);

            CommunityWhoToCallResponse item = CommunityWhoToCallResponse.builder()
                    .id(101L)
                    .department("Maintenance & Repairs")
                    .contactPerson("Priya Sharma")
                    .phoneNumber("9876543210")
                    .isEmergency(false)
                    .isActive(true)
                    .build();

            when(whoToCallService.getActive(1L)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/community/who-to-call"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(101))
                    .andExpect(jsonPath("$[0].department").value("Maintenance & Repairs"))
                    .andExpect(jsonPath("$[0].contactPerson").value("Priya Sharma"));
        }
    }

    @Nested
    @DisplayName("POST /api/community/who-to-call - Create Contact")
    class CreateContact {

        @Test
        @DisplayName("valid payload returns 201 with created contact")
        void valid_returns201() throws Exception {
            Community community = Community.builder().id(1L).name("Test Community").build();
            AppUser user = AppUser.builder().id(10L).fullName("Admin").community(community).build();
            when(loggedInUserService.resolve(any())).thenReturn(user);

            CommunityWhoToCallRequest req = CommunityWhoToCallRequest.builder()
                    .department("Lift Emergency")
                    .contactPerson("Security Officer")
                    .phoneNumber("9876543210")
                    .isEmergency(true)
                    .build();

            CommunityWhoToCallResponse created = CommunityWhoToCallResponse.builder()
                    .id(102L)
                    .department("Lift Emergency")
                    .contactPerson("Security Officer")
                    .phoneNumber("9876543210")
                    .isEmergency(true)
                    .build();

            when(whoToCallService.create(eq(1L), eq(10L), eq("Admin"), any())).thenReturn(created);

            mockMvc.perform(post("/api/community/who-to-call")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(102))
                    .andExpect(jsonPath("$.department").value("Lift Emergency"))
                    .andExpect(jsonPath("$.isEmergency").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/community/who-to-call/history - Audit History")
    class GetHistory {

        @Test
        @DisplayName("returns 200 with change history entries")
        void returns200() throws Exception {
            Community community = Community.builder().id(1L).name("Test Community").build();
            AppUser user = AppUser.builder().id(10L).community(community).build();
            when(loggedInUserService.resolve(any())).thenReturn(user);

            CommunityWhoToCallHistoryResponse hist = CommunityWhoToCallHistoryResponse.builder()
                    .id(1L)
                    .whoToCallId(102L)
                    .communityId(1L)
                    .action("CREATED")
                    .changedByName("Admin")
                    .department("Lift Emergency")
                    .contactPerson("Security Officer")
                    .phoneNumber("9876543210")
                    .changeSummary("Created new contact for Lift Emergency")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(whoToCallService.getAllCommunityHistory(1L)).thenReturn(List.of(hist));

            mockMvc.perform(get("/api/community/who-to-call/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].action").value("CREATED"))
                    .andExpect(jsonPath("$[0].department").value("Lift Emergency"));
        }
    }
}
