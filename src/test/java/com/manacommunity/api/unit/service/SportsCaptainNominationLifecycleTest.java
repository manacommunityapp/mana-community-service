package com.manacommunity.api.unit.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.service.impl.SportsAuctionTeamServiceImpl;
import com.manacommunity.api.sports.service.impl.SportsEventServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Sports Captain Nomination Lifecycle Unit Test")
class SportsCaptainNominationLifecycleTest {

    @Mock private SportsEventRepository eventRepo;
    @Mock private SportsEventRegistrationRepository regRepo;
    @Mock private SportsAuctionConfigRepository auctionConfigRepo;
    @Mock private SportsAuctionTeamRepository auctionTeamRepo;
    @Mock private AppUserRepository userRepo;
    @Mock private com.manacommunity.api.security.AuditService auditService;
    @Mock private com.manacommunity.api.sports.repository.SportsMetaRepository sportMetaRepo;

    private SportsEventServiceImpl sportsEventService;
    private SportsAuctionTeamServiceImpl auctionTeamService;
    private SportsEvent event;
    private AppUser user;
    private SportsEventRegistration registration;

    @BeforeEach
    void setUp() {
        sportsEventService = new SportsEventServiceImpl(
                eventRepo,
                regRepo,
                sportMetaRepo,
                mock(SportsPlayerCategoryRepository.class),
                mock(SportsNotificationSchedulerRepository.class),
                userRepo,
                mock(com.manacommunity.api.repository.CommunityRepository.class),
                mock(SportsVenueRepository.class),
                auctionConfigRepo,
                auctionTeamRepo,
                mock(SportsAuctionPlayerRepository.class),
                mock(SportsTournamentRepository.class),
                mock(com.manacommunity.api.email.RegistrationEmailService.class),
                mock(com.manacommunity.api.service.NotificationManagementService.class),
                mock(com.manacommunity.api.service.RecaptchaService.class),
                mock(com.manacommunity.api.service.OtpService.class),
                mock(com.manacommunity.api.repository.ContactRepository.class),
                mock(SportsEventFormatRepository.class),
                auditService,
                mock(com.manacommunity.api.user.repository.FamilyMemberRepository.class),
                mock(SportsPlayerRankingRepository.class)
        );

        auctionTeamService = new SportsAuctionTeamServiceImpl(
                auctionTeamRepo,
                auctionConfigRepo,
                userRepo,
                auditService,
                mock(com.manacommunity.api.service.NotificationManagementService.class),
                eventRepo
        );

        SportsMeta sport = SportsMeta.builder().id(10L).name("Cricket").build();
        event = SportsEvent.builder()
                .id(771L)
                .name("Cricket Tournament 2026")
                .sport(sport)
                .captainNominationOpen(false)
                .build();

        user = AppUser.builder().id(50L).fullName("Rahul Dravid").build();

        registration = SportsEventRegistration.builder()
                .id(904L)
                .event(event)
                .user(user)
                .captainNomination(false)
                .captainConfirmation(false)
                .build();

        when(eventRepo.findById(771L)).thenReturn(Optional.of(event));
        when(eventRepo.save(any(SportsEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        when(userRepo.findById(50L)).thenReturn(Optional.of(user));

        when(regRepo.findById(904L)).thenReturn(Optional.of(registration));
        when(regRepo.save(any(SportsEventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        SportsAuctionConfig config = SportsAuctionConfig.builder()
                .id(101L)
                .event(event)
                .sport(sport)
                .budgetPerTeam(100000L)
                .build();

        when(auctionConfigRepo.findByEventId(771L)).thenReturn(Optional.of(config));
        when(auctionConfigRepo.save(any(SportsAuctionConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionTeamRepo.findByConfigIdAndOwnerUserId(101L, 50L)).thenReturn(Optional.empty());
        when(auctionTeamRepo.save(any(SportsAuctionTeam.class))).thenAnswer(inv -> {
            SportsAuctionTeam t = inv.getArgument(0);
            if (t.getId() == null) t.setId(505L);
            return t;
        });
    }

    @Test
    @DisplayName("Admin can open and stop captain nominations independently")
    void testAdminCanToggleCaptainNominations() {
        assertThat(event.getCaptainNominationOpen()).isFalse();

        SportsEvent opened = sportsEventService.setCaptainNominationOpen(771L, true);
        assertThat(opened.getCaptainNominationOpen()).isTrue();

        SportsEvent stopped = sportsEventService.setCaptainNominationOpen(771L, false);
        assertThat(stopped.getCaptainNominationOpen()).isFalse();
    }

    @Test
    @DisplayName("Resident cannot nominate when captain nominations are stopped")
    void testResidentCannotNominateWhenStopped() {
        event.setCaptainNominationOpen(false);

        assertThatThrownBy(() -> sportsEventService.nominateCaptain(904L, 50L, false, true, "Team Bangalore"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Captain nominations are currently closed for this event.");

        assertThatThrownBy(() -> auctionTeamService.nominateCaptain(771L, 50L, false, true, "Team Bangalore"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Captain nominations are currently closed for this event.");
    }

    @Test
    @DisplayName("Resident can nominate when captain nominations are open")
    void testResidentCanNominateWhenOpen() {
        event.setCaptainNominationOpen(true);

        SportsEventRegistration regResult = sportsEventService.nominateCaptain(904L, 50L, false, true, "Team Bangalore");
        assertThat(regResult.getCaptainNomination()).isTrue();
        assertThat(regResult.getProposedTeamName()).isEqualTo("Team Bangalore");

        SportsAuctionTeam teamResult = auctionTeamService.nominateCaptain(771L, 50L, false, true, "Team Bangalore");
        assertThat(teamResult.getCaptainNomination()).isTrue();
        assertThat(teamResult.getTeamName()).isEqualTo("Team Bangalore");
    }

    @Test
    @DisplayName("Admin can appoint/nominate captain even when public nominations are closed")
    void testAdminCanNominateEvenWhenStopped() {
        event.setCaptainNominationOpen(false);

        SportsEventRegistration regResult = sportsEventService.nominateCaptain(904L, 999L, true, true, "Admin Appointed Team");
        assertThat(regResult.getCaptainNomination()).isTrue();
        assertThat(regResult.getProposedTeamName()).isEqualTo("Admin Appointed Team");

        SportsAuctionTeam teamResult = auctionTeamService.nominateCaptain(771L, 50L, true, true, "Admin Appointed Team");
        assertThat(teamResult.getCaptainNomination()).isTrue();
        assertThat(teamResult.getTeamName()).isEqualTo("Admin Appointed Team");
    }
}
