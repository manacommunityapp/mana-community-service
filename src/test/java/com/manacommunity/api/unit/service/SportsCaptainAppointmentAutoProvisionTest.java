package com.manacommunity.api.unit.service;

import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.SportsAuctionConfigRepository;
import com.manacommunity.api.sports.repository.SportsAuctionTeamRepository;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.service.impl.SportsAuctionTeamServiceImpl;
import com.manacommunity.api.sports.service.impl.SportsEventServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import com.manacommunity.api.exception.UnauthorizedActionException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Sports Captain Appointment Auto-Provision Unit Test")
class SportsCaptainAppointmentAutoProvisionTest {

    @Nested
    @DisplayName("SportsEventServiceImpl Captain Provisioning")
    class SportsEventServiceTests {
        @Mock private SportsEventRegistrationRepository regRepo;
        @Mock private SportsAuctionConfigRepository auctionConfigRepo;
        @Mock private SportsAuctionTeamRepository auctionTeamRepo;
        @Mock private com.manacommunity.api.security.AuditService auditService;
        @Mock private com.manacommunity.api.sports.repository.SportsMetaRepository sportMetaRepo;

        private SportsEventServiceImpl sportsEventService;
        private SportsEvent event;
        private AppUser user;
        private SportsEventRegistration registration;

        @BeforeEach
        void setUp() {
            sportsEventService = new SportsEventServiceImpl(
                    mock(SportsEventRepository.class),
                    regRepo,
                    sportMetaRepo,
                    mock(com.manacommunity.api.sports.repository.SportsPlayerCategoryRepository.class),
                    mock(com.manacommunity.api.sports.repository.SportsNotificationSchedulerRepository.class),
                    mock(AppUserRepository.class),
                    mock(com.manacommunity.api.repository.CommunityRepository.class),
                    mock(com.manacommunity.api.sports.repository.SportsVenueRepository.class),
                    auctionConfigRepo,
                    auctionTeamRepo,
                    mock(com.manacommunity.api.sports.repository.SportsAuctionPlayerRepository.class),
                    mock(com.manacommunity.api.sports.repository.SportsTournamentRepository.class),
                    mock(com.manacommunity.api.email.RegistrationEmailService.class),
                    mock(com.manacommunity.api.service.NotificationManagementService.class),
                    mock(com.manacommunity.api.service.RecaptchaService.class),
                    mock(com.manacommunity.api.service.OtpService.class),
                    mock(com.manacommunity.api.repository.ContactRepository.class),
                    mock(com.manacommunity.api.sports.repository.SportsEventFormatRepository.class),
                    auditService,
                    mock(com.manacommunity.api.user.repository.FamilyMemberRepository.class),
                    mock(com.manacommunity.api.sports.repository.SportsPlayerRankingRepository.class)
            );

            SportsMeta sport = SportsMeta.builder().id(10L).name("Cricket").build();
            event = SportsEvent.builder().id(771L).name("Cricket Tournament 2026").sport(sport).captainNominationOpen(true).build();
            user = AppUser.builder().id(50L).fullName("Rahul Dravid").build();
            registration = SportsEventRegistration.builder()
                    .id(904L)
                    .event(event)
                    .user(user)
                    .captainNomination(false)
                    .captainConfirmation(false)
                    .build();

            when(regRepo.findById(904L)).thenReturn(Optional.of(registration));
            when(regRepo.save(any(SportsEventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            when(auctionConfigRepo.findByEventId(771L)).thenReturn(Optional.empty());
            when(auctionConfigRepo.save(any(SportsAuctionConfig.class))).thenAnswer(inv -> {
                SportsAuctionConfig c = inv.getArgument(0);
                c.setId(1234L);
                return c;
            });

            when(auctionTeamRepo.findByConfigIdAndOwnerUserId(anyLong(), eq(50L))).thenReturn(Optional.empty());
            when(auctionTeamRepo.save(any(SportsAuctionTeam.class))).thenAnswer(inv -> {
                SportsAuctionTeam t = inv.getArgument(0);
                t.setId(555L);
                return t;
            });
        }

        @Test
        @DisplayName("nominateCaptain auto-provisions SportsAuctionConfig if missing for event")
        void testNominateCaptainAutoProvisionsConfig() {
            SportsEventRegistration result = sportsEventService.nominateCaptain(904L, true, "Team Bangalore");

            assertThat(result).isNotNull();
            assertThat(result.getCaptainNomination()).isTrue();
            assertThat(result.getProposedTeamName()).isEqualTo("Team Bangalore");
            verify(auctionConfigRepo).save(any(SportsAuctionConfig.class));
            verify(auctionTeamRepo).save(any(SportsAuctionTeam.class));
        }

        @Test
        @DisplayName("nominateCaptain succeeds when requesting user owns the registration")
        void testNominateCaptainByOwner() {
            SportsEventRegistration result = sportsEventService.nominateCaptain(904L, 50L, false, true, "Team Bangalore");

            assertThat(result).isNotNull();
            assertThat(result.getCaptainNomination()).isTrue();
            assertThat(result.getProposedTeamName()).isEqualTo("Team Bangalore");
        }

        @Test
        @DisplayName("nominateCaptain throws UnauthorizedActionException when non-admin does not own registration")
        void testNominateCaptainUnauthorized() {
            assertThatThrownBy(() -> sportsEventService.nominateCaptain(904L, 999L, false, true, "Team Bangalore"))
                    .isInstanceOf(UnauthorizedActionException.class)
                    .hasMessageContaining("You can only nominate a captain for your own registration.");
        }

        @Test
        @DisplayName("confirmCaptain auto-provisions config and team if missing")
        void testConfirmCaptainAutoProvisionsConfigAndTeam() {
            SportsEventRegistration result = sportsEventService.confirmCaptain(904L, true);

            assertThat(result).isNotNull();
            assertThat(result.getCaptainConfirmation()).isTrue();
            verify(auctionConfigRepo).save(any(SportsAuctionConfig.class));
            verify(auctionTeamRepo).save(any(SportsAuctionTeam.class));
        }
    }

    @Nested
    @DisplayName("SportsAuctionTeamServiceImpl Captain Provisioning")
    class SportsAuctionTeamServiceTests {
        @Mock private SportsAuctionTeamRepository teamRepo;
        @Mock private SportsAuctionConfigRepository configRepo;
        @Mock private AppUserRepository userRepo;
        @Mock private com.manacommunity.api.security.AuditService auditService;
        @Mock private com.manacommunity.api.service.NotificationManagementService notificationService;
        @Mock private SportsEventRepository eventRepo;

        private SportsAuctionTeamServiceImpl auctionTeamService;
        private SportsEvent event;
        private AppUser user;

        @BeforeEach
        void setUp() {
            auctionTeamService = new SportsAuctionTeamServiceImpl(
                    teamRepo,
                    configRepo,
                    userRepo,
                    auditService,
                    notificationService,
                    eventRepo
            );

            SportsMeta sport = SportsMeta.builder().id(10L).name("Cricket").build();
            event = SportsEvent.builder().id(771L).name("Cricket Tournament 2026").sport(sport).captainNominationOpen(true).build();
            user = AppUser.builder().id(50L).fullName("Rahul Dravid").build();

            when(userRepo.findById(50L)).thenReturn(Optional.of(user));
            when(eventRepo.findById(771L)).thenReturn(Optional.of(event));
            when(configRepo.findByEventId(771L)).thenReturn(Optional.empty());
            when(configRepo.save(any(SportsAuctionConfig.class))).thenAnswer(inv -> {
                SportsAuctionConfig c = inv.getArgument(0);
                c.setId(1234L);
                return c;
            });
            when(teamRepo.findByConfigIdAndOwnerUserId(anyLong(), eq(50L))).thenReturn(Optional.empty());
            when(teamRepo.save(any(SportsAuctionTeam.class))).thenAnswer(inv -> {
                SportsAuctionTeam t = inv.getArgument(0);
                t.setId(555L);
                return t;
            });
        }

        @Test
        @DisplayName("SportsAuctionTeamService.nominateCaptain auto-provisions config if missing")
        void testAuctionTeamServiceNominateCaptainAutoProvisions() {
            SportsAuctionTeam team = auctionTeamService.nominateCaptain(771L, 50L, true, "Warriors");

            assertThat(team).isNotNull();
            assertThat(team.getCaptainNomination()).isTrue();
            assertThat(team.getTeamName()).isEqualTo("Warriors");
            verify(configRepo).save(any(SportsAuctionConfig.class));
            verify(teamRepo).save(any(SportsAuctionTeam.class));
        }
    }
}