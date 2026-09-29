package com.manacommunity.api.slice.controller;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.visitor.controller.VisitorPassController;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.service.VisitorPassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitorPassController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("VisitorPassController Slice Tests")
class VisitorControllerSliceTest extends BaseWebMvcTest {

    @MockitoBean
    private VisitorPassService visitorPassService;

    @MockitoBean
    private LoggedInUserService loggedInUserService;

    @MockitoBean
    private com.manacommunity.api.privacy.PiiMaskingService piiMaskingService;

    @MockitoBean
    private com.manacommunity.api.security.CookieAuthHelper cookieAuthHelper;

    private AppUser mockUser;
    private Community community;
    private VisitorPassResponse samplePass;

    @BeforeEach
    void setUp() {
        community = Community.builder().id(1L).name("Mana Society").build();
        mockUser = AppUser.builder().id(10L).fullName("Alice Resident").flatNo("A-1204").community(community).build();
        when(loggedInUserService.resolve(any())).thenReturn(mockUser);

        samplePass = VisitorPassResponse.builder()
                .id(1L)
                .passCode("MANA-7821")
                .visitorName("Suresh Verma")
                .visitorPhone("9876543210")
                .vehicleNumber("KA-01-MJ-9821")
                .purpose("Family Weekend Dinner")
                .passType("GUEST")
                .status("APPROVED")
                .flatNumber("A-1204")
                .communityId(1L)
                .residentId(10L)
                .build();
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("GET /api/visitors returns 200 with list of visitor passes")
    void getVisitors_returns200() throws Exception {
        when(visitorPassService.getCommunityPasses(1L)).thenReturn(List.of(samplePass));

        mockMvc.perform(get("/api/visitors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passCode").value("MANA-7821"))
                .andExpect(jsonPath("$[0].visitorName").value("Suresh Verma"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("POST /api/visitors/pre-approve creates an expected visitor and returns 201")
    void preApprove_returns201() throws Exception {
        VisitorPassRequest req = new VisitorPassRequest();
        req.setVisitorName("Suresh Verma");
        req.setVisitorPhone("9876543210");
        req.setVehicleNumber("KA-01-MJ-9821");
        req.setPassType("GUEST");
        req.setPurpose("Family Weekend Dinner");

        when(visitorPassService.create(any(), any(), any())).thenReturn(samplePass);

        mockMvc.perform(post("/api/visitors/pre-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passCode").value("MANA-7821"))
                .andExpect(jsonPath("$.visitorName").value("Suresh Verma"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("PUT /api/visitors/{id}/check-in records check-in and returns 200")
    void checkIn_returns200() throws Exception {
        samplePass.setStatus("CHECKED_IN");
        samplePass.setCheckedInAt("2026-09-29 10:00:00");

        when(visitorPassService.checkIn(eq(1L), any(), any(), any())).thenReturn(samplePass);

        mockMvc.perform(put("/api/visitors/1/check-in?gate=Gate 1&guard=Guard Kumar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("PUT /api/visitors/{id}/check-out records departure and returns 200")
    void checkOut_returns200() throws Exception {
        samplePass.setStatus("CHECKED_OUT");
        samplePass.setCheckedOutAt("2026-09-29 12:00:00");

        when(visitorPassService.checkOut(eq(1L), any(), any())).thenReturn(samplePass);

        mockMvc.perform(put("/api/visitors/1/check-out?gate=Gate 1&guard=Guard Kumar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("GET /api/visitors/my-visitors returns resident's visitor history")
    void getMyVisitors_returns200() throws Exception {
        when(visitorPassService.getMyPasses(10L)).thenReturn(List.of(samplePass));

        mockMvc.perform(get("/api/visitors/my-visitors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passCode").value("MANA-7821"))
                .andExpect(jsonPath("$[0].residentId").value(10L));
    }
}
