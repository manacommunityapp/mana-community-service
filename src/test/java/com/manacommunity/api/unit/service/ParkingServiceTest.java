package com.manacommunity.api.unit.service;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.dto.ParkingSpotDto;
import com.manacommunity.api.parking.dto.ReserveSpotRequest;
import com.manacommunity.api.parking.dto.VisitorPassDto;
import com.manacommunity.api.parking.dto.VisitorPassRequest;
import com.manacommunity.api.parking.entity.ParkingSpot;
import com.manacommunity.api.parking.entity.ParkingVisitorPass;
import com.manacommunity.api.parking.repository.ParkingSpotRepository;
import com.manacommunity.api.parking.repository.ParkingVisitorPassRepository;
import com.manacommunity.api.parking.service.impl.ParkingServiceImpl;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParkingService - Unit Tests")
class ParkingServiceTest {

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private ParkingVisitorPassRepository visitorPassRepository;

    @Mock
    private CommunityRepository communityRepository;

    @InjectMocks
    private ParkingServiceImpl parkingService;

    private Community community;
    private AppUser user;
    private ParkingSpot availableSpot;

    @BeforeEach
    void setUp() {
        community = Community.builder()
                .id(1L)
                .name("Mana Community")
                .build();

        user = AppUser.builder()
                .id(101L)
                .fullName("John Doe")
                .email("john@example.com")
                .flatNo("A1-302")
                .community(community)
                .build();

        availableSpot = ParkingSpot.builder()
                .id(10L)
                .community(community)
                .spotNumber("B2-P05")
                .level("Basement 2")
                .spotType("CAR")
                .status("AVAILABLE")
                .build();
    }

    @Test
    @DisplayName("getSpots auto-seeds if repository has 0 spots for community")
    void getSpots_autoSeedsIfEmpty() {
        when(spotRepository.countByCommunityId(1L)).thenReturn(0L);
        when(spotRepository.findByCommunityId(1L)).thenReturn(List.of(availableSpot));

        List<ParkingSpotDto> result = parkingService.getSpots(user, null, null, null);

        assertThat(result).hasSize(1);
        verify(spotRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("getSpots filters by type and status")
    void getSpots_withFilter() {
        ParkingSpot bikeSpot = ParkingSpot.builder()
                .id(11L)
                .community(community)
                .spotNumber("B1-P08")
                .level("Basement 1")
                .spotType("BIKE")
                .status("AVAILABLE")
                .build();

        when(spotRepository.countByCommunityId(1L)).thenReturn(2L);
        when(spotRepository.findByCommunityId(1L)).thenReturn(List.of(availableSpot, bikeSpot));

        List<ParkingSpotDto> result = parkingService.getSpots(user, "CAR", "AVAILABLE", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpotNumber()).isEqualTo("B2-P05");
    }

    @Test
    @DisplayName("getMySpots returns spots assigned to current user")
    void getMySpots_returnsUserSpots() {
        ParkingSpot userSpot = ParkingSpot.builder()
                .id(1L)
                .community(community)
                .spotNumber("B1-P12")
                .level("Basement 1")
                .spotType("CAR")
                .status("OCCUPIED")
                .vehicleNumber("KA-01-AB-1234")
                .assignedUser(user)
                .ownerFlat("A1-302")
                .build();

        when(spotRepository.countByCommunityId(1L)).thenReturn(1L);
        when(spotRepository.findByCommunityIdAndAssignedUserId(1L, 101L)).thenReturn(List.of(userSpot));

        List<ParkingSpotDto> result = parkingService.getMySpots(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwnerName()).isEqualTo("You");
        assertThat(result.get(0).getVehicleNumber()).isEqualTo("KA-01-AB-1234");
    }

    @Test
    @DisplayName("reserveSpot successfully reserves an available spot")
    void reserveSpot_success() {
        ReserveSpotRequest request = ReserveSpotRequest.builder()
                .spotId(10L)
                .vehicleNumber("KA-05-XY-9999")
                .vehicleType("CAR")
                .notes("Owner spot")
                .build();

        when(spotRepository.findById(10L)).thenReturn(Optional.of(availableSpot));
        when(spotRepository.save(any(ParkingSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSpotDto result = parkingService.reserveSpot(user, request);

        assertThat(result.getStatus()).isEqualTo("RESERVED");
        assertThat(result.getVehicleNumber()).isEqualTo("KA-05-XY-9999");
        assertThat(result.getOwnerName()).isEqualTo("You");
    }

    @Test
    @DisplayName("reserveSpot throws InvalidInputException if spot is not AVAILABLE")
    void reserveSpot_throwsWhenNotAvailable() {
        availableSpot.setStatus("OCCUPIED");
        ReserveSpotRequest request = ReserveSpotRequest.builder()
                .spotId(10L)
                .vehicleNumber("KA-05-XY-9999")
                .build();

        when(spotRepository.findById(10L)).thenReturn(Optional.of(availableSpot));

        assertThatThrownBy(() -> parkingService.reserveSpot(user, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("cannot be reserved");
    }

    @Test
    @DisplayName("createVisitorPass generates pass code and records visitor pass")
    void createVisitorPass_success() {
        VisitorPassRequest request = VisitorPassRequest.builder()
                .visitorName("Guest Person")
                .visitorPhone("9876543210")
                .vehicleNumber("KA-04-ZZ-1111")
                .vehicleType("CAR")
                .purpose("Guest Visit")
                .build();

        when(visitorPassRepository.save(any(ParkingVisitorPass.class))).thenAnswer(invocation -> {
            ParkingVisitorPass p = invocation.getArgument(0);
            p.setId(50L);
            return p;
        });

        VisitorPassDto result = parkingService.createVisitorPass(user, request);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getPassCode()).startsWith("VP-");
        assertThat(result.getVisitorName()).isEqualTo("Guest Person");
        assertThat(result.getVehicleNumber()).isEqualTo("KA-04-ZZ-1111");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("createVisitorPass throws InvalidInputException if validUntil is before validFrom")
    void createVisitorPass_throwsWhenInvalidDates() {
        LocalDateTime now = LocalDateTime.now();
        VisitorPassRequest request = VisitorPassRequest.builder()
                .visitorName("Guest Person")
                .vehicleNumber("KA-04-ZZ-1111")
                .validFrom(now)
                .validUntil(now.minusHours(1))
                .build();

        assertThatThrownBy(() -> parkingService.createVisitorPass(user, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Valid until time must be after");
    }
}
