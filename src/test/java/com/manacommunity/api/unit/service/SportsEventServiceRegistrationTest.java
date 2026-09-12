package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.SportsTournament;
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
    @Mock private SportsMetaRepository sportMetaRepo;
    @Mock private SportsPlayerCategoryRepository categoryRepo;
    @Mock private SportsNotificationSchedulerRepository schedulerRepo;
    @Mock private AppUserRepository userRepo;
    @Mock private CommunityRepository communityRepo;
    @Mock private VenueRepository venueRepo;
    @Mock private SportsAuctionConfigRepository auctionConfigRepo;
    @Mock private SportsAuctionTeamRepository auctionTeamRepo;
    @Mock private SportsAuctionPlayerRepository playerRepo;
    @Mock private SportsTournamentRepository tournamentRepo;
    @Mock private com.manacommunity.api.email.RegistrationEmailService registrationEmailService;
    @Mock private com.manacommunity.api.service.NotificationManagementService notificationService;
    @Mock private RecaptchaService recaptchaService;
    @Mock private OtpService otpService;
    @Mock private ContactRepository contactRepository;

    @Mock private com.manacommunity.api.security.AuditService auditService;

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

        SportsTournament tournament = new SportsTournament();
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

    @Test
    void respondToPartnerInvitation_accept_success() {
        AppUser primaryUser = new AppUser();
        primaryUser.setId(10L);
        primaryUser.setFullName("Primary Player");

        AppUser partnerUser = new AppUser();
        partnerUser.setId(20L);
        partnerUser.setFullName("Partner Player");

        SportsEvent event = new SportsEvent();
        event.setId(100L);
        event.setName("Badminton Championship");

        com.manacommunity.api.model.SportsEventRegistration reg = com.manacommunity.api.model.SportsEventRegistration.builder()
                .id(500L)
                .event(event)
                .user(primaryUser)
                .partner(partnerUser)
                .status(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.PENDING)
                .partnerConfirmationStatus(com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.PENDING)
                .build();

        when(regRepo.findById(500L)).thenReturn(Optional.of(reg));
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration result = service.respondToPartnerInvitation(500L, 20L, true, null);

        org.assertj.core.api.Assertions.assertThat(result.getPartnerConfirmationStatus())
                .isEqualTo(com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.CONFIRMED);
        org.assertj.core.api.Assertions.assertThat(result.getPartnerConfirmedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(result.getPartnerDeclineReason()).isNull();

        org.mockito.Mockito.verify(auditService).record(
                org.mockito.ArgumentMatchers.eq(com.manacommunity.api.security.AuditAction.PARTNER_CONFIRMED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("SportsEventRegistration"),
                org.mockito.ArgumentMatchers.eq("500"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void respondToPartnerInvitation_decline_marksRejected() {
        AppUser primaryUser = new AppUser();
        primaryUser.setId(10L);
        primaryUser.setFullName("Primary Player");

        AppUser partnerUser = new AppUser();
        partnerUser.setId(20L);
        partnerUser.setFullName("Partner Player");

        SportsEvent event = new SportsEvent();
        event.setId(100L);
        event.setName("Badminton Championship");

        com.manacommunity.api.model.SportsEventRegistration reg = com.manacommunity.api.model.SportsEventRegistration.builder()
                .id(500L)
                .event(event)
                .user(primaryUser)
                .partner(partnerUser)
                .status(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.PENDING)
                .partnerConfirmationStatus(com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.PENDING)
                .build();

        when(regRepo.findById(500L)).thenReturn(Optional.of(reg));
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration result = service.respondToPartnerInvitation(500L, 20L, false, "Not available this weekend");

        org.assertj.core.api.Assertions.assertThat(result.getPartnerConfirmationStatus())
                .isEqualTo(com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.DECLINED);
        org.assertj.core.api.Assertions.assertThat(result.getStatus())
                .isEqualTo(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.REJECTED);
        org.assertj.core.api.Assertions.assertThat(result.getRejectReason())
                .contains("Not available this weekend");

        org.mockito.Mockito.verify(auditService).record(
                org.mockito.ArgumentMatchers.eq(com.manacommunity.api.security.AuditAction.PARTNER_DECLINED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("SportsEventRegistration"),
                org.mockito.ArgumentMatchers.eq("500"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void respondToPartnerInvitation_unauthorizedPartner_throws() {
        AppUser partnerUser = new AppUser();
        partnerUser.setId(20L);

        com.manacommunity.api.model.SportsEventRegistration reg = com.manacommunity.api.model.SportsEventRegistration.builder()
                .id(500L)
                .partner(partnerUser)
                .status(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.PENDING)
                .partnerConfirmationStatus(com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.PENDING)
                .build();

        when(regRepo.findById(500L)).thenReturn(Optional.of(reg));

        assertThatThrownBy(() -> service.respondToPartnerInvitation(500L, 999L, true, null))
                .isInstanceOf(com.manacommunity.api.exception.UnauthorizedActionException.class)
                .hasMessageContaining("not the designated partner");
    }

    @Test
    void respondToPartnerInvitation_alreadyResponded_throws() {
        AppUser partnerUser = new AppUser();
        partnerUser.setId(20L);

        com.manacommunity.api.model.SportsEventRegistration reg = com.manacommunity.api.model.SportsEventRegistration.builder()
                .id(500L)
                .partner(partnerUser)
                .status(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.PENDING)
                .partnerConfirmationStatus(com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.CONFIRMED)
                .build();

        when(regRepo.findById(500L)).thenReturn(Optional.of(reg));

        assertThatThrownBy(() -> service.respondToPartnerInvitation(500L, 20L, true, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been CONFIRMED");
    }

    @Test
    void getPartnerInvitations_filtersByStatus() {
        com.manacommunity.api.model.SportsEventRegistration reg = new com.manacommunity.api.model.SportsEventRegistration();
        when(regRepo.findByPartnerIdAndPartnerConfirmationStatus(20L, com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.PENDING))
                .thenReturn(List.of(reg));

        List<com.manacommunity.api.model.SportsEventRegistration> result = service.getPartnerInvitations(20L, com.manacommunity.api.model.SportsEventRegistration.PartnerConfirmationStatus.PENDING);
        org.assertj.core.api.Assertions.assertThat(result).hasSize(1);
    }

    @Test
    void registerUser_mixedDoubles_mandatoryTrue_rejectsSameGender() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("MIXED_DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Mixed Doubles Tournament");
        event.setStatus(SportsEvent.EventStatus.REGISTRATION_OPEN);
        event.setSport(sport);
        event.setMandatoryMixedDoubles(true);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Male User 1");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.of(1995, 1, 1));

        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Male User 2");
        partner.setGender("MALE");
        partner.setDateOfBirth(LocalDate.of(1996, 1, 1));

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Mixed Doubles Category");

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.findById(30L)).thenReturn(Optional.of(partner));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("Mixed Doubles requires one Male and one Female player");
    }

    @Test
    void registerUser_mixedDoubles_mandatoryFalse_allowsSameGender() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("MIXED_DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Open Mixed Tournament");
        event.setStatus(SportsEvent.EventStatus.REGISTRATION_OPEN);
        event.setSport(sport);
        event.setMandatoryMixedDoubles(false);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Male User 1");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.of(1995, 1, 1));

        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Male User 2");
        partner.setGender("MALE");
        partner.setDateOfBirth(LocalDate.of(1996, 1, 1));

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Mixed Doubles Category");

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.findById(30L)).thenReturn(Optional.of(partner));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration saved = service.registerUser(req, 2L);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getPartner()).isEqualTo(partner);
    }
}
