package com.manacommunity.api.slice.controller;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.parking.controller.ParkingController;
import com.manacommunity.api.parking.dto.ParkingSpotDto;
import com.manacommunity.api.parking.dto.ReserveSpotRequest;
import com.manacommunity.api.parking.dto.VisitorPassDto;
import com.manacommunity.api.parking.dto.VisitorPassRequest;
import com.manacommunity.api.parking.service.ParkingService;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParkingController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ParkingController Slice Tests")
class ParkingControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private LoggedInUserService loggedInUserService;

    @MockitoBean
    private ParkingService parkingService;

    @MockitoBean
    private com.manacommunity.api.security.CookieAuthHelper cookieAuthHelper;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = AppUser.builder()
                .id(1L)
                .fullName("Test Resident")
                .email("resident@example.com")
                .flatNo("B1-102")
                .build();

        when(loggedInUserService.resolve(any())).thenReturn(mockUser);
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("GET /api/parking/spots returns 200 and list of spots")
    void getSpots_returns200() throws Exception {
        ParkingSpotDto spot = ParkingSpotDto.builder()
                .id(1L)
                .spotNumber("B1-P12")
                .level("Basement 1")
                .type("CAR")
                .status("AVAILABLE")
                .build();

        when(parkingService.getSpots(any(), any(), any(), any())).thenReturn(List.of(spot));

        mockMvc.perform(get("/api/parking/spots")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spotNumber").value("B1-P12"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("POST /api/parking/reserve returns 200 and updated spot")
    void reserveSpot_returns200() throws Exception {
        ReserveSpotRequest req = ReserveSpotRequest.builder()
                .spotId(1L)
                .vehicleNumber("KA-01-AB-1234")
                .vehicleType("CAR")
                .build();

        ParkingSpotDto spot = ParkingSpotDto.builder()
                .id(1L)
                .spotNumber("B1-P12")
                .level("Basement 1")
                .type("CAR")
                .status("RESERVED")
                .vehicleNumber("KA-01-AB-1234")
                .ownerName("Test Resident")
                .build();

        when(parkingService.reserveSpot(any(), any(ReserveSpotRequest.class))).thenReturn(spot);

        mockMvc.perform(post("/api/parking/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotNumber").value("B1-P12"))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.vehicleNumber").value("KA-01-AB-1234"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("GET /api/parking/my-spots returns 200 and user spots")
    void getMySpots_returns200() throws Exception {
        ParkingSpotDto spot = ParkingSpotDto.builder()
                .id(1L)
                .spotNumber("B1-P12")
                .level("Basement 1")
                .type("CAR")
                .status("OCCUPIED")
                .vehicleNumber("KA-01-AB-1234")
                .ownerName("You")
                .build();

        when(parkingService.getMySpots(any())).thenReturn(List.of(spot));

        mockMvc.perform(get("/api/parking/my-spots")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spotNumber").value("B1-P12"))
                .andExpect(jsonPath("$[0].ownerName").value("You"));
    }

    @Test
    @WithMockUserPrincipal(role = "MEMBER")
    @DisplayName("POST /api/parking/visitor-pass returns 201 and created pass")
    void createVisitorPass_returns201() throws Exception {
        VisitorPassRequest req = VisitorPassRequest.builder()
                .visitorName("Guest Name")
                .vehicleNumber("KA-02-CD-5678")
                .vehicleType("CAR")
                .purpose("Guest Visit")
                .build();

        VisitorPassDto pass = VisitorPassDto.builder()
                .id(10L)
                .passCode("VP-123456")
                .visitorName("Guest Name")
                .vehicleNumber("KA-02-CD-5678")
                .vehicleType("CAR")
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusHours(8))
                .status("ACTIVE")
                .build();

        when(parkingService.createVisitorPass(any(), any(VisitorPassRequest.class))).thenReturn(pass);

        mockMvc.perform(post("/api/parking/visitor-pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passCode").value("VP-123456"))
                .andExpect(jsonPath("$.visitorName").value("Guest Name"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
