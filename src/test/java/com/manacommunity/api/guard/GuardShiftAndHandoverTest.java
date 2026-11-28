package com.manacommunity.api.guard;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.guard.dto.GuardShiftResponse;
import com.manacommunity.api.guard.entity.GuardProfile;
import com.manacommunity.api.guard.entity.GuardShift;
import com.manacommunity.api.guard.entity.HandoverItem;
import com.manacommunity.api.guard.entity.ShiftHandover;
import com.manacommunity.api.guard.repository.GuardProfileRepository;
import com.manacommunity.api.guard.repository.GuardShiftRepository;
import com.manacommunity.api.guard.repository.ShiftHandoverRepository;
import com.manacommunity.api.guard.service.GuardService;
import com.manacommunity.api.guard.service.GuardService.GuardClockInVerification;
import com.manacommunity.api.guard.service.ShiftHandoverService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardShiftAndHandoverTest {

    @Mock private ShiftHandoverRepository handoverRepo;
    @Mock private GuardProfileRepository profileRepo;
    @Mock private GuardShiftRepository shiftRepo;
    @Mock private AppUserRepository userRepo;

    private ShiftHandoverService handoverService;
    private GuardService guardService;

    private AppUser nightGuard;
    private ShiftHandover handoverWithOpenItems;
    private GuardShift scheduledShift;

    @BeforeEach
    void setUp() {
        handoverService = new ShiftHandoverService(handoverRepo);
        guardService = new GuardService(profileRepo, shiftRepo, userRepo);

        Community community = Community.builder().id(101L).name("Palm Meadows").type("APARTMENT").build();
        nightGuard = AppUser.builder().id(55L).fullName("Suresh Yadav").community(community).build();

        HandoverItem visitorInside = HandoverItem.builder()
                .id(1L).category("OPEN_VISITOR").description("Plumber Raju still inside B-402").resolvedBefore(false).build();
        HandoverItem uncollectedParcel = HandoverItem.builder()
                .id(2L).category("UNCOLLECTED_PARCEL").description("2 Amazon boxes at Gate-1 rack").resolvedBefore(false).build();
        HandoverItem offlineReader = HandoverItem.builder()
                .id(3L).category("OFFLINE_HARDWARE").description("RFID-READER-02 offline since 18:10").resolvedBefore(false).build();

        handoverWithOpenItems = ShiftHandover.builder()
                .id(700L)
                .community(community)
                .shiftDate(LocalDate.of(2026, 11, 28))
                .shiftType("DAY_TO_NIGHT")
                .status("PENDING")
                .items(List.of(visitorInside, uncollectedParcel, offlineReader))
                .build();

        GuardProfile guardProfile = GuardProfile.builder()
                .id(12L).community(community).fullName("Suresh Yadav").employeeId("GRD-012").assignedGate("MAIN_GATE").build();

        scheduledShift = GuardShift.builder()
                .id(800L)
                .community(community)
                .guard(guardProfile)
                .shiftDate(LocalDate.of(2026, 11, 28))
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(8, 0))
                .gate("MAIN_GATE")
                .status(GuardShift.ShiftStatus.SCHEDULED)
                .build();
    }

    @Test
    void enforcesForcedAcknowledgmentOfAllOpenHandoverItemsBeforeShiftTakeover() {
        when(handoverRepo.findById(700L)).thenReturn(Optional.of(handoverWithOpenItems));
        when(handoverRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Bare acknowledge without acknowledging open items -> blocked
        assertThatThrownBy(() -> handoverService.acknowledge(700L, nightGuard))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("OPEN_VISITOR")
                .hasMessageContaining("UNCOLLECTED_PARCEL")
                .hasMessageContaining("OFFLINE_HARDWARE");

        // 2. Partial acknowledgment (only 2 of 3 open items) -> blocked
        assertThatThrownBy(() -> handoverService.acknowledge(700L, nightGuard, Set.of(1L, 2L)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("OFFLINE_HARDWARE");

        // 3. All 3 open items acknowledged -> transitions to ACKNOWLEDGED
        ShiftHandover completed = handoverService.acknowledge(700L, nightGuard, Set.of(1L, 2L, 3L));
        assertThat(completed.getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(completed.getAcknowledgedBy()).isEqualTo(nightGuard);
    }

    @Test
    void enforcesGpsGeofenceMockDetectionAndSelfieLivenessOnClockIn() {
        // Gate coordinates: Bengaluru 12.9716, 77.5946 (100m geofence)
        double gateLat = 12.9716;
        double gateLon = 77.5946;

        // 1. Mock GPS app active -> blocked immediately
        assertThatThrownBy(() -> guardService.checkInShift(800L,
                new GuardClockInVerification(gateLat, gateLon, gateLat, gateLon, 100.0, true, true, 0.95)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("mock GPS");

        // 2. Guard 2.3 km away (~+0.02 deg lat) -> blocked by geofence
        assertThatThrownBy(() -> guardService.checkInShift(800L,
                new GuardClockInVerification(12.9923, gateLon, gateLat, gateLon, 100.0, false, true, 0.95)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("geofence");

        // 3. Spoofed / low-liveness selfie -> blocked by biometric check
        assertThatThrownBy(() -> guardService.checkInShift(800L,
                new GuardClockInVerification(gateLat + 0.0001, gateLon, gateLat, gateLon, 100.0, false, true, 0.42)))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("liveness");

        // 4. Within ~25m of gate, genuine GPS, high selfie liveness -> IN_PROGRESS
        when(shiftRepo.findById(800L)).thenReturn(Optional.of(scheduledShift));
        when(shiftRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GuardShiftResponse checkedIn = guardService.checkInShift(800L,
                new GuardClockInVerification(12.9718, 77.5947, gateLat, gateLon, 100.0, false, true, 0.92));

        assertThat(checkedIn.status()).isEqualTo("IN_PROGRESS");
        assertThat(checkedIn.checkInTime()).isNotNull();
        assertThat(checkedIn.notes()).contains("Geofence verified");
    }
}
