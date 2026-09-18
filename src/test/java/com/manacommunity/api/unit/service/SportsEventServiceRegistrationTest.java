package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsEventStatus;
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
    private final SportsPlayerRankingRepository rankingRepo = null;
    @Mock private com.manacommunity.api.user.repository.FamilyMemberRepository familyMemberRepository;

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
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setMaxParticipants(null);
        event.setTournament(tournament);
        event.setSport(sport);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Test User");
        user.setGender("MALE");
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
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
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
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
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

    @Test
    void registerUser_historicalAgeConflict_blocksJuniorRegistrationForPriorAdult() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Cricket");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Youth Cricket Cup");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setMinAge(8);
        event.setMaxAge(18);
        event.setSport(sport);

        // User currently claiming to be 10 years old
        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Adult Turned Kid");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.now().minusYears(10));

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Cricket Youth (8 - 18)");
        category.setMinAge(8);
        category.setMaxAge(18);

        // Historical registration with age 33
        com.manacommunity.api.model.SportsEvent pastEvent = new com.manacommunity.api.model.SportsEvent();
        pastEvent.setName("Summer Cricket Cup 2025");
        com.manacommunity.api.model.SportsEventRegistration pastReg = new com.manacommunity.api.model.SportsEventRegistration();
        pastReg.setEvent(pastEvent);
        pastReg.setAge(33);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());
        when(regRepo.findByUserId(2L)).thenReturn(List.of(pastReg));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("Age conflict detected: Historical records show prior participation in Adult categories");
    }

    @Test
    void registerUser_familyMember_successWithChildAge() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("parent@example.com");
        req.setFamilyMemberId(100L);

        SportsMeta sport = new SportsMeta();
        sport.setName("Cricket");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Youth Cricket Cup");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setMinAge(8);
        event.setMaxAge(18);
        event.setSport(sport);

        // Parent user (35 yrs)
        AppUser parent = new AppUser();
        parent.setId(2L);
        parent.setFullName("Sandeep Parent");
        parent.setEmail("parent@example.com");
        parent.setFlatNo("B-402");
        parent.setDateOfBirth(LocalDate.now().minusYears(35));

        // Child family member (10 yrs)
        com.manacommunity.api.user.model.FamilyMember child = com.manacommunity.api.user.model.FamilyMember.builder()
                .id(100L)
                .user(parent)
                .name("Aarav Child")
                .relation("Son")
                .gender("MALE")
                .dob(LocalDate.now().minusYears(10).toString())
                .age(10)
                .build();

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Cricket Youth (8 - 18)");
        category.setMinAge(8);
        category.setMaxAge(18);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(parent));
        when(familyMemberRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.of(child));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration saved = service.registerUser(req, 2L);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getPlayerName()).isEqualTo("Aarav Child");
        org.assertj.core.api.Assertions.assertThat(saved.getAge()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(saved.getRelation()).isEqualTo("Son");
        org.assertj.core.api.Assertions.assertThat(saved.getFamilyMember()).isEqualTo(child);
    }

    @Test
    void registerUser_familyMember_alreadyRegistered_throwsException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("parent@example.com");
        req.setFamilyMemberId(100L);

        SportsMeta sport = new SportsMeta();
        sport.setName("Cricket");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Youth Cricket Cup");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setMinAge(8);
        event.setMaxAge(18);
        event.setSport(sport);

        AppUser parent = new AppUser();
        parent.setId(2L);
        parent.setFullName("Sandeep Parent");
        parent.setEmail("parent@example.com");
        parent.setFlatNo("B-402");
        parent.setDateOfBirth(LocalDate.now().minusYears(35));

        com.manacommunity.api.user.model.FamilyMember child = com.manacommunity.api.user.model.FamilyMember.builder()
                .id(100L)
                .user(parent)
                .name("Aarav Child")
                .relation("Son")
                .gender("MALE")
                .dob(LocalDate.now().minusYears(10).toString())
                .age(10)
                .build();

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(parent));
        when(familyMemberRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.of(child));
        when(regRepo.existsByEventIdAndFamilyMemberIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.anyList())).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.AlreadyRegisteredException.class)
                .hasMessageContaining("Aarav Child (Son) has already been registered for this event.");
    }

    @Test
    void registerUser_missingDateOfBirth_throwsInvalidInputException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Summer League");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("User No DOB");
        user.setGender("MALE");
        user.setDateOfBirth(null);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("Please update your Date of Birth in your Profile");
    }

    @Test
    void registerUser_missingGender_throwsInvalidInputException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Summer League");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("User No Gender");
        user.setGender(null);
        user.setDateOfBirth(LocalDate.of(1995, 5, 20));

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("Please update your Gender in your Profile");
    }

    @Test
    void registerUser_missingBothGenderAndDob_throwsInvalidInputException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Summer League");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("User Incomplete");
        user.setGender(null);
        user.setDateOfBirth(null);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("Please update your Gender and Date of Birth in your Profile");
    }

    @Test
    void registerUser_familyMember_withPartnerFamilyMember_success() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("DOUBLES");
        req.setEmail("parent@example.com");
        req.setFamilyMemberId(100L);
        req.setPartnerFamilyMemberId(200L);

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Junior Badminton Cup");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setMinAge(4);
        event.setMaxAge(18);
        event.setSport(sport);

        AppUser parent1 = new AppUser();
        parent1.setId(2L);
        parent1.setFullName("Parent One");
        parent1.setDateOfBirth(LocalDate.of(1988, 1, 1));

        AppUser parent2 = new AppUser();
        parent2.setId(5L);
        parent2.setFullName("Parent Two");
        parent2.setDateOfBirth(LocalDate.of(1989, 1, 1));

        com.manacommunity.api.user.model.FamilyMember child1 = com.manacommunity.api.user.model.FamilyMember.builder()
                .id(100L)
                .user(parent1)
                .name("Aarav Child")
                .relation("Son")
                .gender("MALE")
                .age(10)
                .build();

        com.manacommunity.api.user.model.FamilyMember child2 = com.manacommunity.api.user.model.FamilyMember.builder()
                .id(200L)
                .user(parent2)
                .name("Rohan Partner Child")
                .relation("Son")
                .gender("MALE")
                .age(11)
                .build();

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Badminton Boys Under 12");
        category.setMinAge(4);
        category.setMaxAge(11);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(parent1));
        when(familyMemberRepository.findByIdAndUserId(100L, 2L)).thenReturn(Optional.of(child1));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(child2));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration saved = service.registerUser(req, 2L);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getFamilyMember()).isEqualTo(child1);
        org.assertj.core.api.Assertions.assertThat(saved.getPartnerFamilyMember()).isEqualTo(child2);
        org.assertj.core.api.Assertions.assertThat(saved.getPartner()).isEqualTo(parent2);
    }

    @Test
    void registerUser_partnerOverageForCategory_throwsAgeMismatchException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        // Tournament allows 4 to 100
        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Badminton Open Tournament");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setMinAge(4);
        event.setMaxAge(100);
        event.setSport(sport);

        // Primary player is 10
        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Junior Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.now().minusYears(10));

        // Partner is 25 (adult)
        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Adult Partner");
        partner.setGender("MALE");
        partner.setDateOfBirth(LocalDate.now().minusYears(25));

        // Category is strictly Under 12 (4-11)
        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Badminton Boys Under 12");
        category.setMinAge(4);
        category.setMaxAge(11);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.findById(30L)).thenReturn(Optional.of(partner));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.AgeMismatchException.class)
                .hasMessageContaining("Badminton Boys Under 12 (Partner)");
    }

    @Test
    void registerUser_mensDoubles_rejectsFemalePartner() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Badminton Championship");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setSport(sport);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Male Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.of(1995, 1, 1));

        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Female Partner");
        partner.setGender("FEMALE");
        partner.setDateOfBirth(LocalDate.of(1996, 1, 1));

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Badminton Men");
        category.setGender("MALE");

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.findById(30L)).thenReturn(Optional.of(partner));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("Badminton Men requires both players to be Male");
    }

    @Test
    void registerUser_openDoubles_allowsAnyGenderCombination() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Carroms");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Carroms Open Doubles");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setSport(sport);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Male Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.of(1995, 1, 1));

        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Female Partner");
        partner.setGender("FEMALE");
        partner.setDateOfBirth(LocalDate.of(1996, 1, 1));

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Carroms Open Doubles (15+)");
        category.setGender("ALL");

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

    @Test
    void registerUser_ageOutsideCategoryLimits_throwsAgeMismatchException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("junior@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Badminton Junior Tournament");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setSport(sport);
        event.setMinAge(5);
        event.setMaxAge(60);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Senior Player");
        user.setGender("MALE");
        // User is 25 years old
        user.setDateOfBirth(LocalDate.now().minusYears(25));

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Under 14 Boys");
        category.setMinAge(6);
        category.setMaxAge(14);
        category.setGender("MALE");

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.AgeMismatchException.class)
                .hasMessageContaining("Under 14 Boys")
                .hasMessageContaining("25 yrs");
    }

    @Test
    void registerUser_categoryNotEligibleForEvent_throwsInvalidInputException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(20L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        com.manacommunity.api.model.SportsPlayerCategory eventCategory = new com.manacommunity.api.model.SportsPlayerCategory();
        eventCategory.setId(10L);
        eventCategory.setName("Adults Men (18-40)");

        com.manacommunity.api.model.SportsPlayerCategory otherCategory = new com.manacommunity.api.model.SportsPlayerCategory();
        otherCategory.setId(20L);
        otherCategory.setName("Veterans (50+)");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Badminton Adults Only");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setSport(sport);
        event.setCategories(java.util.Set.of(eventCategory));

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Adult Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.now().minusYears(25));

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepo.findById(20L)).thenReturn(Optional.of(otherCategory));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("not eligible for event");
    }

    @Test
    void registerUser_partnerMissingDob_throwsInvalidInputException() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Tennis");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Tennis Doubles");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setSport(sport);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Primary Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.of(1995, 1, 1));

        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Partner Without DOB");
        partner.setGender("MALE");
        partner.setDateOfBirth(null); // Missing DOB

        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Men Doubles (18+)");
        category.setMinAge(18);
        category.setMaxAge(60);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.findById(30L)).thenReturn(Optional.of(partner));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.InvalidInputException.class)
                .hasMessageContaining("must update their Date of Birth");
    }

    @Test
    void registerUser_playingUpInHigherAgeCategory_succeeds() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(1L);
        req.setCategoryId(10L);
        req.setMatchType("SINGLES");
        req.setEmail("youngpro@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setName("Badminton");

        SportsEvent event = new SportsEvent();
        event.setId(1L);
        event.setName("Badminton Open Youth Championship");
        event.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event.setSport(sport);
        event.setAllowHigherAgeCategory(true);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Young Prodigy");
        user.setGender("MALE");
        // User is 12 years old
        user.setDateOfBirth(LocalDate.now().minusYears(12));

        // Category is U16 (minAge 14, maxAge 16)
        com.manacommunity.api.model.SportsPlayerCategory category = new com.manacommunity.api.model.SportsPlayerCategory();
        category.setId(10L);
        category.setName("Under 16 Boys");
        category.setMinAge(14);
        category.setMaxAge(16);
        category.setGender("MALE");

        when(eventRepo.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(category));
        when(regRepo.findByEventId(1L)).thenReturn(List.of());
        when(regRepo.findByUserId(2L)).thenReturn(List.of());
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration saved = service.registerUser(req, 2L);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getAge()).isEqualTo(12);
        org.assertj.core.api.Assertions.assertThat(saved.getCategory().getName()).isEqualTo("Under 16 Boys");
    }

    @Test
    void registerUser_optionB_duplicateSameMatchFormatInSameTournament_isBlocked() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(2L);
        req.setCategoryId(20L);
        req.setMatchType("SINGLES");
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setId(5L);
        sport.setName("Badminton");

        SportsTournament tournament = new SportsTournament();
        tournament.setId(100L);
        tournament.setName("Badminton Annual Tournament");

        // Event 2: U16 Singles in Tournament 100
        SportsEvent event2 = new SportsEvent();
        event2.setId(2L);
        event2.setName("Badminton U16 Singles");
        event2.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event2.setSport(sport);
        event2.setTournament(tournament);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Multi Category Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.now().minusYears(14));

        com.manacommunity.api.model.SportsPlayerCategory category2 = new com.manacommunity.api.model.SportsPlayerCategory();
        category2.setId(20L);
        category2.setName("Under 16 Boys");
        category2.setMinAge(14);
        category2.setMaxAge(16);
        category2.setGender("MALE");

        // Existing registration in Event 1 (U14 Singles) for the SAME tournament and SAME format (SINGLES)
        SportsEvent event1 = new SportsEvent();
        event1.setId(1L);
        event1.setName("Badminton U14 Singles");
        event1.setSport(sport);
        event1.setTournament(tournament);

        com.manacommunity.api.model.SportsPlayerCategory category1 = new com.manacommunity.api.model.SportsPlayerCategory();
        category1.setName("Under 14 Boys");

        com.manacommunity.api.model.SportsEventRegistration existingReg = new com.manacommunity.api.model.SportsEventRegistration();
        existingReg.setEvent(event1);
        existingReg.setCategory(category1);
        existingReg.setMatchType(SportsEvent.MatchFormat.SINGLES);
        existingReg.setStatus(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.CONFIRMED);

        when(eventRepo.findById(2L)).thenReturn(Optional.of(event2));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepo.findById(20L)).thenReturn(Optional.of(category2));
        when(regRepo.findByEventId(2L)).thenReturn(List.of());
        when(regRepo.findByUserId(2L)).thenReturn(List.of(existingReg));

        assertThatThrownBy(() -> service.registerUser(req, 2L))
                .isInstanceOf(com.manacommunity.api.exception.AlreadyRegisteredException.class)
                .hasMessageContaining("already registered for Badminton SINGLES in 'Badminton U14 Singles'")
                .hasMessageContaining("cannot enter multiple age categories of the same match format");
    }

    @Test
    void registerUser_optionB_differentMatchFormatsInSameTournament_isAllowed() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEventId(2L);
        req.setCategoryId(20L);
        req.setMatchType("DOUBLES");
        req.setPartnerUserId(30L);
        req.setEmail("player@example.com");

        SportsMeta sport = new SportsMeta();
        sport.setId(5L);
        sport.setName("Badminton");

        SportsTournament tournament = new SportsTournament();
        tournament.setId(100L);
        tournament.setName("Badminton Annual Tournament");

        // Event 2: Doubles in Tournament 100
        SportsEvent event2 = new SportsEvent();
        event2.setId(2L);
        event2.setName("Badminton Open Doubles");
        event2.setStatus(SportsEventStatus.REGISTRATION_OPEN);
        event2.setSport(sport);
        event2.setTournament(tournament);
        event2.setAllowMultipleCategories(true);

        AppUser user = new AppUser();
        user.setId(2L);
        user.setFullName("Multi Format Player");
        user.setGender("MALE");
        user.setDateOfBirth(LocalDate.now().minusYears(20));

        AppUser partner = new AppUser();
        partner.setId(30L);
        partner.setFullName("Partner Player");
        partner.setGender("MALE");
        partner.setDateOfBirth(LocalDate.now().minusYears(22));

        com.manacommunity.api.model.SportsPlayerCategory category2 = new com.manacommunity.api.model.SportsPlayerCategory();
        category2.setId(20L);
        category2.setName("Open Doubles");
        category2.setMinAge(16);
        category2.setMaxAge(50);
        category2.setGender("ALL");

        // Existing registration in Event 1: SINGLES format in same tournament
        SportsEvent event1 = new SportsEvent();
        event1.setId(1L);
        event1.setName("Badminton Men Singles");
        event1.setSport(sport);
        event1.setTournament(tournament);

        com.manacommunity.api.model.SportsEventRegistration existingReg = new com.manacommunity.api.model.SportsEventRegistration();
        existingReg.setEvent(event1);
        existingReg.setMatchType(SportsEvent.MatchFormat.SINGLES);
        existingReg.setStatus(com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.CONFIRMED);

        when(eventRepo.findById(2L)).thenReturn(Optional.of(event2));
        doNothing().when(recaptchaService).verify(null, null);
        doNothing().when(otpService).assertEmailVerified(anyString());
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.findById(30L)).thenReturn(Optional.of(partner));
        when(categoryRepo.findById(20L)).thenReturn(Optional.of(category2));
        when(regRepo.findByEventId(2L)).thenReturn(List.of());
        when(regRepo.findByUserId(2L)).thenReturn(List.of(existingReg));
        when(regRepo.findByUserId(30L)).thenReturn(List.of());
        when(regRepo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        com.manacommunity.api.model.SportsEventRegistration saved = service.registerUser(req, 2L);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getMatchType()).isEqualTo(SportsEvent.MatchFormat.DOUBLES);
        org.assertj.core.api.Assertions.assertThat(saved.getPartner()).isEqualTo(partner);
    }
}
