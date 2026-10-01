package com.manacommunity.api.parking.unit;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.dto.ParkingSlotRequest;
import com.manacommunity.api.parking.dto.ParkingSlotResponse;
import com.manacommunity.api.parking.entity.ParkingSlot;
import com.manacommunity.api.parking.repository.ParkingSlotRepository;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.parking.service.ParkingSlotService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Parking Slot Service Unit Tests")
class ParkingSlotServiceTest {

    @Mock
    private ParkingSlotRepository slotRepository;

    @Mock
    private ResidentVehicleRepository vehicleRepository;

    @InjectMocks
    private ParkingSlotService parkingSlotService;

    private Community testCommunity;
    private AppUser testUser;
    private ParkingSlot testSlot;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(1L).name("Mana Residency").build();
        testUser = AppUser.builder().id(5L).fullName("Suresh Raina").community(testCommunity).build();
        testSlot = ParkingSlot.builder()
                .id(100L)
                .community(testCommunity)
                .slotNumber("P-101")
                .zone("North Wing")
                .floor("Basement 1")
                .slotType(ParkingSlot.SlotType.COVERED)
                .status(ParkingSlot.SlotStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get all available slots")
    void shouldGetAvailableSlots() {
        when(slotRepository.findByCommunityIdAndStatusOrderBySlotNumberAsc(1L, ParkingSlot.SlotStatus.AVAILABLE))
                .thenReturn(List.of(testSlot));

        List<ParkingSlotResponse> slots = parkingSlotService.getAvailableSlots(1L);
        assertEquals(1, slots.size());
        assertEquals("P-101", slots.get(0).slotNumber());
    }

    @Test
    @DisplayName("Should create slot successfully")
    void shouldCreateSlot() {
        ParkingSlotRequest req = new ParkingSlotRequest("P-102", "South Wing", "B1", "COVERED", "Near elevator");
        when(slotRepository.findByCommunityIdAndSlotNumber(1L, "P-102")).thenReturn(Optional.empty());
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> {
            ParkingSlot s = inv.getArgument(0);
            s.setId(102L);
            return s;
        });

        ParkingSlotResponse res = parkingSlotService.createSlot(testUser, req);
        assertNotNull(res);
        assertEquals("P-102", res.slotNumber());
    }

    @Test
    @DisplayName("Should reject duplicate slot number creation")
    void shouldRejectDuplicateSlot() {
        ParkingSlotRequest req = new ParkingSlotRequest("P-101", "North Wing", "B1", "COVERED", "");
        when(slotRepository.findByCommunityIdAndSlotNumber(1L, "P-101")).thenReturn(Optional.of(testSlot));

        assertThrows(InvalidInputException.class, () -> parkingSlotService.createSlot(testUser, req));
    }

    @Test
    @DisplayName("Should assign slot to user")
    void shouldAssignSlot() {
        when(slotRepository.findById(100L)).thenReturn(Optional.of(testSlot));
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        ParkingSlotResponse res = parkingSlotService.assignSlot(100L, 5L, testUser);
        assertNotNull(res);
        assertEquals("OCCUPIED", res.status());
        assertEquals(5L, res.assignedToId());
    }

    @Test
    @DisplayName("Should release slot back to available status")
    void shouldReleaseSlot() {
        testSlot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        testSlot.setAssignedTo(testUser);
        when(slotRepository.findById(100L)).thenReturn(Optional.of(testSlot));
        when(slotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        ParkingSlotResponse res = parkingSlotService.releaseSlot(100L);
        assertNotNull(res);
        assertEquals("AVAILABLE", res.status());
        assertNull(res.assignedToId());
    }
}
