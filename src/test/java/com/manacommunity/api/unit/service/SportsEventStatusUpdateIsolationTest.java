package com.manacommunity.api.unit.service;

import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsEventStatus;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import com.manacommunity.api.sports.service.impl.SportsEventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SportsEvent Status Update Isolation Unit Test")
class SportsEventStatusUpdateIsolationTest {

    @Mock private SportsEventRepository eventRepo;
    @Mock private SportsTournamentRepository tournamentRepo;
    @Mock private com.manacommunity.api.service.NotificationManagementService notificationService;
    @Mock private com.manacommunity.api.sports.repository.SportsEventRegistrationRepository regRepo;

    @InjectMocks
    private SportsEventServiceImpl sportsEventService;

    private SportsTournament tournament;
    private SportsEvent event1;
    private SportsEvent event2;

    @BeforeEach
    void setUp() {
        tournament = SportsTournament.builder()
                .id(100L)
                .name("Annual Badminton Championship")
                .registrationStatus(SportsEventStatus.DRAFT)
                .sportsEvents(new ArrayList<>())
                .build();

        event1 = SportsEvent.builder()
                .id(1L)
                .name("Men Doubles 18+")
                .status(SportsEventStatus.DRAFT)
                .active(true)
                .tournament(tournament)
                .build();

        event2 = SportsEvent.builder()
                .id(2L)
                .name("Women Singles 18+")
                .status(SportsEventStatus.DRAFT)
                .active(true)
                .tournament(tournament)
                .build();

        tournament.getSportsEvents().add(event1);
        tournament.getSportsEvents().add(event2);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event1));
        when(eventRepo.findById(2L)).thenReturn(Optional.of(event2));
        when(eventRepo.save(any(SportsEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tournamentRepo.save(any(SportsTournament.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Starting registration for event1 opens event1, does NOT open sibling event2, and activates DRAFT tournament")
    void testOpenSingleEventDoesNotAffectSibling() {
        SportsEvent result = sportsEventService.updateStatus(1L, "REGISTRATION_OPEN");

        assertThat(result.getStatus()).isEqualTo(SportsEventStatus.REGISTRATION_OPEN);
        assertThat(result.getActive()).isTrue();

        // Sibling event2 MUST remain in DRAFT
        assertThat(event2.getStatus()).isEqualTo(SportsEventStatus.DRAFT);

        // Tournament should be activated from DRAFT to REGISTRATION_OPEN
        assertThat(tournament.getRegistrationStatus()).isEqualTo(SportsEventStatus.REGISTRATION_OPEN);
        verify(tournamentRepo).save(tournament);
    }

    @Test
    @DisplayName("Closing event1 does NOT close sibling event2 or tournament when event2 is still open")
    void testCloseSingleEventLeavesSiblingAndTournamentOpen() {
        // Setup: tournament is open, event1 is open, event2 is open
        tournament.setRegistrationStatus(SportsEventStatus.REGISTRATION_OPEN);
        event1.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event2.setStatus(SportsEventStatus.REGISTRATION_OPEN);

        SportsEvent result = sportsEventService.updateStatus(1L, "REGISTRATION_CLOSED");

        assertThat(result.getStatus()).isEqualTo(SportsEventStatus.REGISTRATION_CLOSED);

        // Sibling event2 MUST still be REGISTRATION_OPEN
        assertThat(event2.getStatus()).isEqualTo(SportsEventStatus.REGISTRATION_OPEN);

        // Tournament MUST remain REGISTRATION_OPEN because event2 is still open
        assertThat(tournament.getRegistrationStatus()).isEqualTo(SportsEventStatus.REGISTRATION_OPEN);
    }

    @Test
    @DisplayName("Closing event1 when all other events are already closed updates tournament status to closed")
    void testCloseLastEventClosesTournament() {
        // Setup: event2 is already closed
        tournament.setRegistrationStatus(SportsEventStatus.REGISTRATION_OPEN);
        event1.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event2.setStatus(SportsEventStatus.REGISTRATION_CLOSED);

        SportsEvent result = sportsEventService.updateStatus(1L, "REGISTRATION_CLOSED");

        assertThat(result.getStatus()).isEqualTo(SportsEventStatus.REGISTRATION_CLOSED);

        // Since both event1 and event2 are now closed, tournament status is closed
        assertThat(tournament.getRegistrationStatus()).isEqualTo(SportsEventStatus.REGISTRATION_CLOSED);
        verify(tournamentRepo).save(tournament);
    }
}
