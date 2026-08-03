package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.Tournament;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.RecaptchaService;
import com.manacommunity.api.service.OtpService;
import com.manacommunity.api.service.impl.SportsEventServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportsEventServiceRegistrationTest {

    @Mock private SportsEventRepository eventRepo;
    @Mock private SportsEventRegistrationRepository regRepo;
    @Mock private SportMetaRepository sportMetaRepo;
    @Mock private PlayerCategoryRepository categoryRepo;
    @Mock private SportsNotificationSchedulerRepository schedulerRepo;
    @Mock private AppUserRepository userRepo;
    @Mock private CommunityRepository communityRepo;
    @Mock private VenueRepository venueRepo;
    @Mock private AuctionConfigRepository auctionConfigRepo;
    @Mock private AuctionTeamRepository auctionTeamRepo;
    @Mock private AuctionPlayerRepository playerRepo;
    @Mock private TournamentRepository tournamentRepo;
    @Mock private com.manacommunity.api.email.RegistrationEmailService registrationEmailService;
    @Mock private com.manacommunity.api.service.NotificationManagementService notificationService;
    @Mock private RecaptchaService recaptchaService;
    @Mock private OtpService otpService;
    @Mock private ContactRepository contactRepository;

    @InjectMocks
    private SportsEventServiceImpl service;

    @Test
    void registerUser_usesTournamentMaxParticipants_whenEventLimitIsNull() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Cricket");

        Tournament tournament = new Tournament();
        tournament.setMaxParticipants(3);

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Weekend League");
        event.setStatus(SportsEvent.EventStatus.REGISTRATION_OPEN);
        event.setMaxParticipants(null);
        event.setTournament(tournament);
        event.setSport(sport);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Test User");
        user.setDateOfBirth(LocalDate.of(1995, 1, 1));

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());
        when(regRepo.countByEventId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(EventFullException.class)
                .hasMessageContaining("3");
    }
}
