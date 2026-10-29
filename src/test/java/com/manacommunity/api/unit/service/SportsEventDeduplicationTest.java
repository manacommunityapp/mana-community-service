package com.manacommunity.api.unit.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.sports.dto.SportsEventRequest;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsMeta;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.repository.SportsMetaRepository;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import com.manacommunity.api.sports.service.impl.SportsEventServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SportsEvent Deduplication Unit Test")
class SportsEventDeduplicationTest {

    @Mock private SportsEventRepository eventRepo;
    @Mock private SportsMetaRepository sportMetaRepo;
    @Mock private SportsTournamentRepository tournamentRepo;
    @Mock private CommunityRepository communityRepo;
    @Mock private AppUserRepository userRepo;
    @Mock private com.manacommunity.api.security.AuditService auditService;

    @InjectMocks
    private SportsEventServiceImpl sportsEventService;

    private SportsMeta mockSport;
    private Community mockCommunity;
    private AppUser mockUser;
    private SportsTournament mockTournament;

    @BeforeEach
    void setUp() {
        mockSport = SportsMeta.builder().id(10L).name("Badminton").build();
        mockCommunity = Community.builder().id(2L).name("Test Community").build();
        mockUser = AppUser.builder().id(99L).email("admin@test.com").build();
        mockTournament = SportsTournament.builder()
                .id(19L)
                .name("LE 2026 Season Fest")
                .eventDateStart(LocalDate.now().plusDays(1))
                .eventDateEnd(LocalDate.now().plusDays(10))
                .build();

        when(sportMetaRepo.findById(10L)).thenReturn(Optional.of(mockSport));
        when(communityRepo.findById(2L)).thenReturn(Optional.of(mockCommunity));
        when(userRepo.getReferenceById(99L)).thenReturn(mockUser);
        when(tournamentRepo.findById(19L)).thenReturn(Optional.of(mockTournament));
    }

    @Test
    @DisplayName("returns existing event without inserting duplicate when called with identical parameters within threshold")
    void returnsExistingEventWhenRecentDuplicateExists() {
        SportsEventRequest req = new SportsEventRequest();
        req.setName("Badminton — Men Singles");
        req.setSportId(10L);
        req.setCommunityId(2L);
        req.setTournamentId(19L);
        req.setEventDateStart(LocalDate.now().plusDays(2));
        req.setEventDateEnd(LocalDate.now().plusDays(5));

        SportsEvent existing = SportsEvent.builder()
                .id(501L)
                .name("Badminton — Men Singles")
                .sport(mockSport)
                .community(mockCommunity)
                .tournament(mockTournament)
                .createdAt(LocalDateTime.now().minusSeconds(2))
                .build();

        when(eventRepo.findRecentDuplicates(eq(2L), eq(10L), eq("Badminton — Men Singles"), eq(19L), any()))
                .thenReturn(List.of(existing));

        SportsEvent result = sportsEventService.createEvent(req, 99L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(501L);
        assertThat(result.getName()).isEqualTo("Badminton — Men Singles");

        // Verify that eventRepo.save was NEVER called because duplicate was caught!
        verify(eventRepo, never()).save(any(SportsEvent.class));
    }

    @Test
    @DisplayName("creates and saves new event when no recent duplicate exists")
    void createsNewEventWhenNoDuplicateExists() {
        SportsEventRequest req = new SportsEventRequest();
        req.setName("Badminton — Women Doubles");
        req.setSportId(10L);
        req.setCommunityId(2L);
        req.setTournamentId(19L);
        req.setEventDateStart(LocalDate.now().plusDays(2));
        req.setEventDateEnd(LocalDate.now().plusDays(5));

        when(eventRepo.findRecentDuplicates(eq(2L), eq(10L), eq("Badminton — Women Doubles"), eq(19L), any()))
                .thenReturn(Collections.emptyList());

        SportsEvent createdEvent = SportsEvent.builder()
                .id(502L)
                .name("Badminton — Women Doubles")
                .sport(mockSport)
                .community(mockCommunity)
                .tournament(mockTournament)
                .build();

        when(eventRepo.save(any(SportsEvent.class))).thenReturn(createdEvent);

        SportsEvent result = sportsEventService.createEvent(req, 99L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(502L);

        // Verify that eventRepo.save was called once
        verify(eventRepo, times(1)).save(any(SportsEvent.class));
    }
}
