package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.repository.AppUserRepository;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.dto.SportsEventRequest;
import com.manacommunity.api.dto.SportsNotificationScheduleDto;
import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.dto.SponsorDto;
import com.manacommunity.api.exception.*;
import com.manacommunity.api.email.RegistrationEmailService;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.service.SportsEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.repository.ContactRepository;
import com.manacommunity.api.model.Contact;
import com.manacommunity.api.dto.ContactDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventServiceImpl implements SportsEventService {

    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final SportsMetaRepository sportMetaRepo;
    private final SportsPlayerCategoryRepository categoryRepo;
    private final SportsNotificationSchedulerRepository schedulerRepo;
    private final AppUserRepository userRepo;
    private final CommunityRepository communityRepo;
    private final VenueRepository venueRepo;
    private final SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsAuctionTeamRepository auctionTeamRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsTournamentRepository tournamentRepo;
    private final RegistrationEmailService registrationEmailService;
    private final NotificationManagementService notificationService;
    private final com.manacommunity.api.service.RecaptchaService recaptchaService;
    private final com.manacommunity.api.service.OtpService otpService;
    private final ContactRepository contactRepository;
    private final com.manacommunity.api.repository.SportsEventFormatRepository formatRepo;
    private final AuditService auditService;
    private final com.manacommunity.api.user.repository.FamilyMemberRepository familyMemberRepository;
    private final SportsPlayerRankingRepository rankingRepo;

    private java.util.List<Contact> resolveContacts(java.util.List<ContactDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new java.util.ArrayList<>();
        return dtos.stream().map(dto -> {
            if (dto.getId() != null) {
                return contactRepository.findById(dto.getId())
                        .orElseGet(() -> contactRepository.save(Contact.builder()
                                .name(dto.getName()).title(dto.getTitle())
                                .number(dto.getNumber()).email(dto.getEmail()).build()));
            }
            return contactRepository.findByNameAndNumberAndEmail(dto.getName(), dto.getNumber(), dto.getEmail())
                    .orElseGet(() -> contactRepository.save(Contact.builder()
                            .name(dto.getName()).title(dto.getTitle())
                            .number(dto.getNumber()).email(dto.getEmail()).build()));
        }).collect(java.util.stream.Collectors.toList());
    }

    private Integer resolveMaxParticipants(SportsEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getMaxParticipants() != null) {
            return event.getMaxParticipants();
        }
        if (event.getTournament() != null && event.getTournament().getMaxParticipants() != null) {
            return event.getTournament().getMaxParticipants();
        }
        return null;
    }

    @Transactional
    public SportsEvent createEvent(SportsEventRequest req, Long adminUserId) {
        SportsMeta sport = sportMetaRepo.findById(req.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport", req.getSportId()));

        Venue venue = null;
        if (req.getVenueId() != null) {
            venue = venueRepo.findById(req.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId()));
        }

        SportsEvent event = SportsEvent.builder()
                .name(req.getName())
                .active(true)
                .sport(sport)
                .community(communityRepo.findById(req.getCommunityId()).orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId())))
                .eventDateStart(req.getEventDateStart())
                .eventDateEnd(req.getEventDateEnd())
                .registrationDateStart(req.getRegistrationDateStart())
                .registrationDateEnd(req.getRegistrationDateEnd())
                .venue(venue)
                .maxParticipants(req.getMaxParticipants() != null ? req.getMaxParticipants() : 64)
                .status(SportsEventStatus.DRAFT)
                .tournamentType(req.getTournamentType() != null
                        ? SportsEvent.TournamentType.valueOf(req.getTournamentType()) : null)
                .createdBy(userRepo.getReferenceById(adminUserId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .disputeCommittee(resolveDisputeCommittee(req.getDisputeCommitteeIds()))
                .minPlayers(req.getMinPlayers())
                .maxPlayers(req.getMaxPlayers())
                .gender(req.getGender())
                .playersBorn(req.getPlayersBorn())
                .contactName(req.getContactName())
                .contactNumber(req.getContactNumber())
                .contactEmail(req.getContactEmail())
                .contacts(resolveContacts(req.getContacts()))
                .otherContacts(req.getOtherContacts())
                .auctionEnabled(req.getAuction() != null ? req.getAuction() : (req.getAuctionEnabled() != null && req.getAuctionEnabled()))
                .bannerImage(req.getBannerImage())
                .tournamentLevel(req.getTournamentLevel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .dueTime(req.getDueTime())
                .minAge(req.getMinAge() != null ? req.getMinAge() : 0)
                .maxAge(req.getMaxAge() != null ? req.getMaxAge() : 100)
                .adminApprovalRequired(req.getAdminApprovalRequired() == null || req.getAdminApprovalRequired())
                .mandatoryMixedDoubles(req.getMandatoryMixedDoubles() == null || req.getMandatoryMixedDoubles())
                .allowHigherAgeCategory(req.getAllowHigherAgeCategory() == null || req.getAllowHigherAgeCategory())
                .allowMultipleCategories(req.getAllowMultipleCategories() == null || req.getAllowMultipleCategories())
                .build();

        event.setFormat(parseMatchFormats(req.getFormat()));

        if (req.getCategoryIds() != null)
            event.setCategories(new java.util.HashSet<>(categoryRepo.findAllById(req.getCategoryIds())));

        if (req.getSponsors() != null) {
            List<SportsEventSponsor> sponsorsList = new java.util.ArrayList<>();
            for (SponsorDto s : req.getSponsors()) {
                sponsorsList.add(SportsEventSponsor.builder()
                        .event(event)
                        .category(s.getCategory())
                        .name(s.getName())
                        .url(s.getUrl())
                        .build());
            }
            event.setSponsors(sponsorsList);
        }

        if (req.getTournamentId() != null) {
            SportsTournament tournament = tournamentRepo.findById(req.getTournamentId())
                    .orElseThrow(() -> new ResourceNotFoundException("SportsTournament", req.getTournamentId()));
            event.setTournament(tournament);
            validateEventDatesWithinTournament(req.getEventDateStart(), req.getEventDateEnd(), tournament);
        }

        SportsEvent saved = eventRepo.save(event);

        if (req.getNotifications() != null)
            scheduleNotifications(saved, req.getNotifications());

        auditService.record(
            AuditAction.SPORTS_EVENT_CREATED,
            AuditModule.SPORTS,
            "SportsEvent",
            String.valueOf(saved.getId()),
            null,
            "name=" + saved.getName() + ", sport=" + (saved.getSport() != null ? saved.getSport().getName() : "")
        );

        return saved;
    }

    @Transactional
    public SportsEventRegistration registerUser(RegistrationRequest req, Long userId) {
        SportsEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        if (event.getStatus() != SportsEventStatus.REGISTRATION_OPEN)
            throw new RegistrationClosedException(event.getName(), event.getStatus().name());

        // Anti-abuse gates (both no-ops unless enabled in config): bot check then
        // proof the registrant controls the email they're submitting.
        recaptchaService.verify(req.getRecaptchaToken(), null);
        otpService.assertEmailVerified(req.getEmail());

        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Resolve participant identity (either User self, or FamilyMember)
        String pName;
        String email = req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail() : user.getEmail();
        String flat = req.getFlatNumber() != null && !req.getFlatNumber().isBlank() ? req.getFlatNumber() : user.getFlatNo();
        String relation = req.getRelation();
        int age;
        String gender = user.getGender();
        com.manacommunity.api.user.model.FamilyMember familyMember = null;

        if (req.getFamilyMemberId() != null) {
            com.manacommunity.api.user.model.FamilyMember member = familyMemberRepository.findByIdAndUserId(req.getFamilyMemberId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", req.getFamilyMemberId()));
            familyMember = member;
            pName = member.getName();
            relation = member.getRelation();
            gender = member.getGender();
            if (gender == null || gender.trim().isEmpty()) {
                throw new InvalidInputException("Please update Gender for family member " + pName + " in your Profile before registering for sports events.");
            }
            if (member.getEmail() != null && !member.getEmail().isBlank()) {
                email = member.getEmail();
            }
            if (member.getDob() != null && !member.getDob().isBlank()) {
                try {
                    LocalDate memberDob = LocalDate.parse(member.getDob().trim());
                    age = Period.between(memberDob, LocalDate.now()).getYears();
                } catch (Exception e) {
                    age = member.getAge() != null ? member.getAge() : (req.getAge() != null ? req.getAge() : 0);
                }
            } else if (member.getAge() != null) {
                age = member.getAge();
            } else if (req.getAge() != null) {
                age = req.getAge();
            } else {
                throw new InvalidInputException("Please update Date of Birth for family member " + pName + " in your Profile before registering for sports events.");
            }
        } else {
            pName = req.getPlayerName() != null && !req.getPlayerName().isBlank() ? req.getPlayerName() : user.getFullName();

            boolean missingDob = user.getDateOfBirth() == null;
            boolean missingGender = user.getGender() == null || user.getGender().trim().isEmpty();

            if (missingDob && missingGender) {
                throw new InvalidInputException("Please update your Gender and Date of Birth in your Profile before registering for sports events.");
            } else if (missingDob) {
                throw new InvalidInputException("Please update your Date of Birth in your Profile before registering for sports events.");
            } else if (missingGender) {
                throw new InvalidInputException("Please update your Gender in your Profile before registering for sports events.");
            }

            gender = user.getGender();
            age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        }

        List<SportsEventRegistration.RegistrationStatus> activeStatuses = List.of(
                SportsEventRegistration.RegistrationStatus.PENDING,
                SportsEventRegistration.RegistrationStatus.REGISTERED,
                SportsEventRegistration.RegistrationStatus.CONFIRMED
        );

        SportsPlayerCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsPlayerCategory", req.getCategoryId()));

        // Check if category is valid for this specific event (if event specifies categories)
        if (event.getCategories() != null && !event.getCategories().isEmpty()) {
            boolean validForEvent = event.getCategories().stream()
                    .anyMatch(c -> c.getId().equals(category.getId()));
            if (!validForEvent) {
                throw new InvalidInputException(
                        "Category '" + category.getName() + "' is not eligible for event '" + event.getName() + "'."
                );
            }
        }

        // 1 & 2. Validate participant age against Event & Category age rules
        validateAgeEligibility(age, event, category, null);

        // 3. Validate against Category Gender restrictions
        String catGender = category.getGender();
        if (catGender == null || catGender.isBlank()) {
            String cText = (category.getName() != null ? category.getName() : "").toLowerCase();
            // Female check first — must precede male check to avoid "women" matching the male "men" substring
            // Possessive & plurals: "women's", "womens", "girl's", "girls", "females", etc.
            if (cText.matches(".*\\b(womens?|woman|females?|girls?|ladies)('s)?\\b.*")) {
                catGender = "FEMALE";
            } else if (cText.matches(".*(?<![a-z])(mens?|man\\b|males?|boys?|gentlemen)('s)?(?![a-z]).*")) {
                catGender = "MALE";
            }
        }
        if (catGender != null && !catGender.isBlank()) {
            String cg = catGender.trim().toUpperCase();
            if (!"ALL".equals(cg) && !"ANY".equals(cg) && !"OPEN".equals(cg) && !"MIXED".equals(cg)) {
                String pGender = gender != null ? gender.trim().toUpperCase() : "";
                boolean maleCat = "MALE".equals(cg) || "MEN".equals(cg) || "BOYS".equals(cg) || "M".equals(cg);
                boolean femaleCat = "FEMALE".equals(cg) || "WOMEN".equals(cg) || "GIRLS".equals(cg) || "F".equals(cg);
                boolean malePlayer = "MALE".equals(pGender) || "MEN".equals(pGender) || "BOYS".equals(pGender) || "BOY".equals(pGender) || "M".equals(pGender);
                boolean femalePlayer = "FEMALE".equals(pGender) || "WOMEN".equals(pGender) || "GIRLS".equals(pGender) || "GIRL".equals(pGender) || "F".equals(pGender);

                if ((maleCat && !malePlayer) || (femaleCat && !femalePlayer)) {
                    throw new InvalidInputException(
                            "Gender mismatch: Category '" + category.getName() + "' is restricted to " + cg +
                            ", but player's gender is " + (gender != null && !gender.isBlank() ? gender : "unspecified") + "."
                    );
                }
            }
        }

        // 3b. Validate against Event-level Gender restrictions
        String eventGender = event.getGender();
        if (eventGender != null && !eventGender.isBlank()) {
            String eg = eventGender.trim().toUpperCase();
            if (!"ALL".equals(eg) && !"ANY".equals(eg) && !"OPEN".equals(eg) && !"MIXED".equals(eg)) {
                String pGender = gender != null ? gender.trim().toUpperCase() : "";
                boolean maleEvent = "MALE".equals(eg) || "MEN".equals(eg) || "BOYS".equals(eg) || "M".equals(eg);
                boolean femaleEvent = "FEMALE".equals(eg) || "WOMEN".equals(eg) || "GIRLS".equals(eg) || "F".equals(eg);
                boolean malePlayer = "MALE".equals(pGender) || "M".equals(pGender);
                boolean femalePlayer = "FEMALE".equals(pGender) || "F".equals(pGender);

                if ((maleEvent && !malePlayer) || (femaleEvent && !femalePlayer)) {
                    throw new InvalidInputException(
                            "Gender mismatch: Event '" + event.getName() + "' is restricted to " + eg +
                            ", but player's gender is " + (gender != null && !gender.isBlank() ? gender : "unspecified") + "."
                    );
                }
            }
        }

        // 4. Historical Age Conflict Detection (Layer 2)
        // If registering self (not via family member, and pName matches user) as a junior (< 18),
        // but historical records show prior participation as adult (18+)
        boolean isSelfRegistration = req.getFamilyMemberId() == null && normEq(pName, user.getFullName());
        if (isSelfRegistration && age < 18) {
            boolean hasAdultHistory = regRepo.existsByUserIdAndAgeGreaterThanEqual(userId, 18);
            if (hasAdultHistory) {
                throw new InvalidInputException(
                        "Age conflict detected: Historical records show prior participation in Adult categories. " +
                        "To register your child/family member, please add them via Family Members."
                );
            }
        }

        SportsEvent.MatchFormat matchFormat = null;
        com.manacommunity.api.model.SportsEventFormat selectedFormat = null;

        if (req.getFormatId() != null) {
            selectedFormat = formatRepo.findById(req.getFormatId())
                    .orElseThrow(() -> new ResourceNotFoundException("SportsEventFormat", req.getFormatId()));
            if (!selectedFormat.getEvent().getId().equals(event.getId())) {
                throw new InvalidInputException("The selected format does not belong to this event.");
            }
            matchFormat = selectedFormat.getFormat();
        } else if (req.getMatchType() != null && !req.getMatchType().isBlank()) {
            try {
                matchFormat = SportsEvent.MatchFormat.valueOf(req.getMatchType().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        if (matchFormat == null) {
            String catName = category.getName() != null ? category.getName().toUpperCase() : "";
            if (catName.contains("MIXED")) {
                matchFormat = SportsEvent.MatchFormat.MIXED_DOUBLES;
            } else if (req.getPartnerUserId() != null || req.getPartnerFamilyMemberId() != null || catName.contains("DOUBLES")) {
                matchFormat = SportsEvent.MatchFormat.DOUBLES;
            } else {
                matchFormat = SportsEvent.MatchFormat.SINGLES;
            }
        }

        if (selectedFormat == null && matchFormat != null) {
            final SportsEvent.MatchFormat targetFmt = matchFormat;
            if (event.getEventFormats() != null && !event.getEventFormats().isEmpty()) {
                selectedFormat = event.getEventFormats().stream()
                        .filter(f -> f.getFormat() == targetFmt)
                        .findFirst()
                        .orElse(null);
            }
            if (selectedFormat == null) {
                selectedFormat = formatRepo.findByEventIdAndFormat(event.getId(), targetFmt).orElse(null);
            }
        }

        // Validate that the requested match format is configured for this event
        if (matchFormat != null && event.getEventFormats() != null && !event.getEventFormats().isEmpty()) {
            final SportsEvent.MatchFormat checkFmt = matchFormat;
            boolean formatAvailable = event.getEventFormats().stream()
                    .anyMatch(f -> f.getFormat() == checkFmt);
            if (!formatAvailable && selectedFormat == null) {
                String available = event.getEventFormats().stream()
                        .map(f -> f.getFormat().name().replace('_', ' '))
                        .collect(java.util.stream.Collectors.joining(", "));
                throw new InvalidInputException(
                        checkFmt.name().replace('_', ' ') + " is not available for event '" + event.getName()
                        + "'. Available formats: " + available + "."
                );
            }
        }

        // Family member explicit duplicate guard (format-aware)
        if (familyMember != null) {
            boolean memberAlreadyRegistered = regRepo.existsByEventIdAndFamilyMemberIdAndMatchTypeAndStatusIn(
                    req.getEventId(), familyMember.getId(), matchFormat, activeStatuses);
            if (memberAlreadyRegistered) {
                throw new AlreadyRegisteredException(
                        familyMember.getName() + " (" + (familyMember.getRelation() != null ? familyMember.getRelation() : "Family Member") + ") has already been registered for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }
        }

        String finalEmail = email;
        String finalFlat = flat;
        String finalPlayerName = pName;

        boolean duplicate = regRepo.existsDuplicateRegistration(
                req.getEventId(), finalPlayerName, finalEmail, finalFlat, matchFormat);
        if (duplicate) {
            throw new AlreadyRegisteredException(
                    "Registration for " + pName
                            + " (" + (email != null && !email.isBlank() ? email : "no email") + ", "
                            + (flat != null && !flat.isBlank() ? flat : "no flat") + ") already exists"
                            + (matchFormat != null ? " for " + matchFormat.name().replace('_', ' ') : "") + ".");
        }

        long currentCount = regRepo.countByEventId(req.getEventId());
        Integer maxParticipants = resolveMaxParticipants(event);
        if (maxParticipants != null && currentCount >= maxParticipants) {
            throw new EventFullException(event.getName(), maxParticipants);
        }

        // Option B: Check for duplicate registration in the same match format across the tournament/sport
        validateOptionBDuplicates(user, familyMember, pName, event, category, matchFormat, activeStatuses);

        // Partner validation & duplicate guard
        AppUser partner = null;
        com.manacommunity.api.user.model.FamilyMember partnerFamilyMember = null;
        String partnerDisplayName = null;
        String partnerGender = "";

        if (req.getPartnerFamilyMemberId() != null) {
            partnerFamilyMember = familyMemberRepository.findById(req.getPartnerFamilyMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partner FamilyMember", req.getPartnerFamilyMemberId()));

            if (familyMember != null && familyMember.getId().equals(partnerFamilyMember.getId())) {
                throw new InvalidInputException("You cannot select the same family member as a doubles partner.");
            }

            partner = partnerFamilyMember.getUser() != null ? partnerFamilyMember.getUser() : user;
            partnerDisplayName = partnerFamilyMember.getName();
            partnerGender = partnerFamilyMember.getGender() != null ? partnerFamilyMember.getGender().trim().toUpperCase() : "";

            // Check if partner family member is already partner in another registration for this format
            boolean partnerFamilyAlreadySelected = regRepo.existsByEventIdAndPartnerFamilyMemberIdAndMatchTypeAndStatusIn(
                    req.getEventId(), partnerFamilyMember.getId(), matchFormat, activeStatuses);
            if (partnerFamilyAlreadySelected) {
                throw new AlreadyRegisteredException(
                        partnerDisplayName + " is already registered / pending as a partner for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }

            // Check if partner family member is already a primary registrant for this format
            boolean partnerFamilyAlreadyPrimary = regRepo.existsByEventIdAndFamilyMemberIdAndMatchTypeAndStatusIn(
                    req.getEventId(), partnerFamilyMember.getId(), matchFormat, activeStatuses);
            if (partnerFamilyAlreadyPrimary) {
                throw new AlreadyRegisteredException(
                        partnerDisplayName + " has already registered as a primary player for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }

            int partnerAge = 0;
            if (partnerFamilyMember.getDob() != null && !partnerFamilyMember.getDob().isBlank()) {
                try {
                    LocalDate pDob = LocalDate.parse(partnerFamilyMember.getDob().trim());
                    partnerAge = Period.between(pDob, LocalDate.now()).getYears();
                } catch (Exception e) {
                    partnerAge = partnerFamilyMember.getAge() != null ? partnerFamilyMember.getAge() : 0;
                }
            } else {
                partnerAge = partnerFamilyMember.getAge() != null ? partnerFamilyMember.getAge() : 0;
            }
            validateAgeEligibility(partnerAge, event, category, "Partner");

            // Partner Option B duplicate guard
            validateOptionBDuplicates(partner, partnerFamilyMember, partnerDisplayName, event, category, matchFormat, activeStatuses);
        } else if (req.getPartnerUserId() != null) {
            // Self as partner check
            if (req.getPartnerUserId().equals(userId)) {
                throw new InvalidInputException("You cannot select yourself as a doubles partner.");
            }

            partner = userRepo.findById(req.getPartnerUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getPartnerUserId()));
            partnerDisplayName = partner.getFullName();
            partnerGender = partner.getGender() != null ? partner.getGender().trim().toUpperCase() : "";

            // 1. Partner cannot already be partner in another registration for THIS event in the same format
            boolean partnerAlreadySelected = regRepo.existsByEventIdAndPartnerIdAndMatchTypeAndStatusIn(
                    req.getEventId(), req.getPartnerUserId(), matchFormat, activeStatuses);
            if (partnerAlreadySelected) {
                throw new AlreadyRegisteredException(
                        partner.getFullName() + " is already registered / pending as a partner for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }

            // 2. Partner cannot already be a primary registrant for THIS event in the same format
            boolean partnerAlreadyPrimary = regRepo.existsByEventIdAndUserIdAndMatchTypeAndStatusIn(
                    req.getEventId(), req.getPartnerUserId(), matchFormat, activeStatuses);
            if (partnerAlreadyPrimary) {
                throw new AlreadyRegisteredException(
                        partner.getFullName() + " has already registered for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }

            // 3. Partner age bounds validation (event and category limits)
            if (partner.getDateOfBirth() == null) {
                throw new InvalidInputException("Selected partner " + partnerDisplayName + " must update their Date of Birth in their Profile before registering for sports events.");
            }
            int partnerAge = Period.between(partner.getDateOfBirth(), LocalDate.now()).getYears();
            validateAgeEligibility(partnerAge, event, category, "Partner");

            // 4. Partner Option B duplicate guard
            validateOptionBDuplicates(partner, null, partnerDisplayName, event, category, matchFormat, activeStatuses);
        }

        // Check if the primary registering participant is already someone else's partner for this format
        if (familyMember != null) {
            boolean memberAlreadyPartner = regRepo.existsByEventIdAndPartnerFamilyMemberIdAndMatchTypeAndStatusIn(
                    req.getEventId(), familyMember.getId(), matchFormat, activeStatuses);
            if (memberAlreadyPartner) {
                throw new AlreadyRegisteredException(
                        familyMember.getName() + " has already been selected as a partner in another registration for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }
        } else if (partner != null) {
            boolean userAlreadyPartner = regRepo.existsByEventIdAndPartnerIdAndMatchTypeAndStatusIn(
                    req.getEventId(), userId, matchFormat, activeStatuses);
            if (userAlreadyPartner) {
                throw new AlreadyRegisteredException(
                        "You have already been selected as a partner in another registration for this event"
                                + (matchFormat != null ? " in " + matchFormat.name().replace('_', ' ') : "") + ".");
            }
        }

        // Doubles & Mixed Doubles Gender Validation
        if ((matchFormat == SportsEvent.MatchFormat.DOUBLES || matchFormat == SportsEvent.MatchFormat.MIXED_DOUBLES) && partner != null) {
            String doublesCatGender = category.getGender() != null ? category.getGender().trim().toUpperCase() : "ALL";
            String primaryGender = gender != null ? gender.trim().toUpperCase() : (user.getGender() != null ? user.getGender().trim().toUpperCase() : "");

            if (!primaryGender.isEmpty() && !partnerGender.isEmpty()) {
                // 1. Male-only Category Doubles (e.g. Men Doubles, Boys Doubles)
                if ("MALE".equals(doublesCatGender)) {
                    if (!"MALE".equals(primaryGender) || !"MALE".equals(partnerGender)) {
                        throw new InvalidInputException(
                                category.getName() + " requires both players to be Male. Selected: " +
                                pName + " (" + primaryGender + ") and " + partnerDisplayName + " (" + partnerGender + ")."
                        );
                    }
                }
                // 2. Female-only Category Doubles (e.g. Women Doubles, Girls Doubles)
                else if ("FEMALE".equals(doublesCatGender)) {
                    if (!"FEMALE".equals(primaryGender) || !"FEMALE".equals(partnerGender)) {
                        throw new InvalidInputException(
                                category.getName() + " requires both players to be Female. Selected: " +
                                pName + " (" + primaryGender + ") and " + partnerDisplayName + " (" + partnerGender + ")."
                        );
                    }
                }
                // 3. Mixed Doubles (1 Male + 1 Female mandatory when configured)
                else if ("MIXED".equals(doublesCatGender) || matchFormat == SportsEvent.MatchFormat.MIXED_DOUBLES) {
                    boolean isMandatoryMixed = event.getMandatoryMixedDoubles() == null || event.getMandatoryMixedDoubles();
                    if (isMandatoryMixed) {
                        boolean isValidMixed = ("MALE".equals(primaryGender) && "FEMALE".equals(partnerGender))
                                || ("FEMALE".equals(primaryGender) && "MALE".equals(partnerGender));
                        if (!isValidMixed) {
                            throw new InvalidInputException(
                                    "Mixed Doubles requires one Male and one Female player. Selected: " +
                                    pName + " (" + primaryGender + ") and " + partnerDisplayName + " (" + partnerGender + ")."
                            );
                        }
                    }
                }
                // 4. Open / ALL Category (e.g. Open Doubles 15+, Junior Open Doubles)
                // Allows all combinations: Male+Male, Female+Female, Male+Female, Female+Male without restriction
            }
        }

        // Admin-approval toggle: when the event requires vetting, the entry lands
        // PENDING and an organiser must confirm it; otherwise it auto-confirms.
        boolean approvalRequired = event.getAdminApprovalRequired() == null || event.getAdminApprovalRequired();
        SportsEventRegistration.RegistrationStatus initialStatus = approvalRequired
                ? SportsEventRegistration.RegistrationStatus.PENDING
                : SportsEventRegistration.RegistrationStatus.CONFIRMED;

        SportsEventRegistration reg = SportsEventRegistration.builder()
                .event(event)
                .community(event.getCommunity())
                .user(user)
                .familyMember(familyMember)
                .category(category)
                .matchType(matchFormat)
                .eventFormat(selectedFormat)
                .partner(partner)
                .partnerFamilyMember(partnerFamilyMember)
                .partnerConfirmationStatus(partner != null ? SportsEventRegistration.PartnerConfirmationStatus.PENDING : null)
                .status(initialStatus)
                .playerName(pName)
                .email(email)
                .relation(relation != null ? relation : req.getRelation())
                .flatNumber(flat)
                .age(age)
                .role(req.getRole())
                .registeredAt(LocalDateTime.now())
                .build();

        SportsEventRegistration saved = regRepo.save(reg);

        if (!approvalRequired) {
            handleConfirmationSideEffects(saved);
        }

        // Email the right stage: "we received your entry" (pending) vs. "you're confirmed".
        registrationEmailService.send(saved, approvalRequired
                ? RegistrationEmailService.Stage.RECEIVED
                : RegistrationEmailService.Stage.CONFIRMED);

        if (partner != null) {
            try {
                String dates = event.getEventDateStart() != null ? event.getEventDateStart().toString() : "TBA";
                if (event.getEventDateEnd() != null && !event.getEventDateEnd().equals(event.getEventDateStart())) {
                    dates += " to " + event.getEventDateEnd();
                }
                String body;
                if (partnerFamilyMember != null) {
                    body = user.getFullName() + " invited your " + (partnerFamilyMember.getRelation() != null ? partnerFamilyMember.getRelation() : "family member")
                            + " (" + partnerFamilyMember.getName() + ") as doubles partner for " + event.getName()
                            + " (" + category.getName() + "). Dates: " + dates + ". Please confirm participation.";
                } else {
                    body = user.getFullName() + " invited you as their doubles partner for " + event.getName()
                            + " (" + category.getName() + "). Dates: " + dates + ". Please confirm your participation.";
                }
                String metadata = String.format("{\"registrationId\":%d,\"eventId\":%d,\"partnerConfirmationStatus\":\"PENDING\",\"primaryPlayer\":\"%s\"}",
                        saved.getId(), event.getId(), user.getFullName().replace("\"", "\\\""));

                notificationService.createNotification(
                        partner.getId(),
                        NotificationType.PARTNER_SELECTED,
                        NotificationCategory.EVENTS,
                        "Doubles Partner Invitation — " + event.getName(),
                        body,
                        "/sports?tab=my-sports&confirmPartner=" + saved.getId(),
                        ReferenceType.SPORTS_EVENT,
                        event.getId(),
                        NotificationPriority.HIGH,
                        metadata,
                        event.getCommunity() != null ? event.getCommunity().getId() : null
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch partner notification for registration {}", saved.getId(), e);
            }
        }

        auditService.record(
            AuditAction.PLAYER_REGISTERED,
            AuditModule.SPORTS,
            "SportsEventRegistration",
            String.valueOf(saved.getId()),
            null,
            "eventId=" + event.getId() + ", player=" + saved.getPlayerName() + ", status=" + saved.getStatus()
        );

        return saved;
    }

    private static List<SportsEvent.MatchFormat> parseMatchFormats(String csv) {
        if (csv == null || csv.isBlank()) return new java.util.ArrayList<>();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SportsEvent.MatchFormat::valueOf)
                .collect(java.util.stream.Collectors.toList());
    }

    /** Null-safe, case-insensitive, trimmed equality (treats null and blank as equal). */
    private static boolean normEq(String a, String b) {
        return java.util.Objects.equals(
                a == null ? "" : a.trim().toLowerCase(),
                b == null ? "" : b.trim().toLowerCase());
    }

    @Transactional
    public void withdraw(Long registrationId, Long userId) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));

        if (!reg.getUser().getId().equals(userId))
            throw new UnauthorizedActionException("You can only withdraw your own registration.");

        reg.setStatus(SportsEventRegistration.RegistrationStatus.WITHDRAWN);
        regRepo.save(reg);

        try {
            String eventName = reg.getEvent() != null ? reg.getEvent().getName() : "the event";
            notificationService.createNotification(
                    userId, NotificationType.REGISTRATION_WITHDRAWN, NotificationCategory.EVENTS,
                    "Registration Withdrawn",
                    "Your registration for " + eventName + " has been withdrawn",
                    null, ReferenceType.SPORTS_EVENT,
                    reg.getEvent() != null ? reg.getEvent().getId() : null,
                    NotificationPriority.NORMAL, null, null);
        } catch (Exception e) {
            log.warn("Failed to persist withdrawal notification", e);
        }
    }

    @Override
    public List<SportsEventRegistration> getEventRegistrations(Long eventId) {
        List<SportsEventRegistration> regs = regRepo.findByEventId(eventId);
        hydrateCaptaincy(regs, eventId);
        return regs;
    }

    @Override
    public List<SportsEventRegistration> getUserRegistrations(Long userId) {
        List<SportsEventRegistration> regs = regRepo.findByUserId(userId);
        // Hydrate each registration with its event's captaincy info
        for (SportsEventRegistration reg : regs) {
            hydrateCaptaincy(List.of(reg), reg.getEvent().getId());
        }
        return regs;
    }

    @Override
    @Transactional
    public SportsEventRegistration confirmRegistration(Long registrationId) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));
        reg.setStatus(SportsEventRegistration.RegistrationStatus.CONFIRMED);
        reg.setReviewedAt(LocalDateTime.now());
        SportsEventRegistration saved = regRepo.save(reg);

        handleConfirmationSideEffects(saved);

        // Registration process — the entry is now CONFIRMED.
        registrationEmailService.send(saved, RegistrationEmailService.Stage.CONFIRMED);

        auditService.record(
            AuditAction.REGISTRATION_APPROVED,
            AuditModule.SPORTS,
            "SportsEventRegistration",
            String.valueOf(saved.getId()),
            "status=PENDING",
            "status=CONFIRMED"
        );

        return saved;
    }

    private void handleConfirmationSideEffects(SportsEventRegistration saved) {
        auctionConfigRepo.findByEventId(saved.getEvent().getId()).ifPresent(config -> {
            boolean exists = playerRepo.findByConfigId(config.getId()).stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(saved.getUser().getId()));
            if (!exists) {
                String cat = "Batsmen";
                if ("Bowler".equalsIgnoreCase(saved.getRole())) cat = "Bowler";
                else if ("All-rounder".equalsIgnoreCase(saved.getRole())) cat = "All-rounder";
                else if ("Wicket Keeper".equalsIgnoreCase(saved.getRole())) cat = "Wicket Keeper";

                long count = playerRepo.countByConfigId(config.getId());
                SportsAuctionPlayer player = SportsAuctionPlayer.builder()
                        .config(config)
                        .community(config.getCommunity() != null ? config.getCommunity() : (saved.getEvent() != null ? saved.getEvent().getCommunity() : null))
                        .user(saved.getUser())
                        .playerName(saved.getPlayerName() != null && !saved.getPlayerName().isEmpty() ? saved.getPlayerName() : saved.getUser().getFullName())
                        .category(cat)
                        .playerRole(saved.getRole() != null ? saved.getRole() : "Batsman")
                        .age(saved.getAge() != null ? saved.getAge() : 30)
                        .basePrice(config.getBasePrice() != null ? config.getBasePrice() : 1000)
                        .rating(saved.getUser() != null && saved.getEvent() != null && saved.getEvent().getSport() != null && saved.getEvent().getCommunity() != null
                                ? rankingRepo.findByUserIdAndSportIdAndCommunityId(
                                        saved.getUser().getId(),
                                        saved.getEvent().getSport().getId(),
                                        saved.getEvent().getCommunity().getId())
                                        .map(r -> r.getRating()).orElse(null)
                                : null)
                        .statsJson("{\"matches\":24,\"runs\":620,\"wickets\":18}")
                        .queueOrder((int) count + 1)
                        .status(SportsAuctionPlayer.PlayerStatus.QUEUED)
                        .uploadedAt(LocalDateTime.now())
                        .build();
                playerRepo.save(player);
            }
        });

        hydrateCaptaincy(List.of(saved), saved.getEvent().getId());
    }

    @Override
    @Transactional
    public SportsEventRegistration rejectRegistration(Long registrationId, String reason) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));
        reg.setStatus(SportsEventRegistration.RegistrationStatus.REJECTED);
        reg.setRejectReason(reason);
        reg.setReviewedAt(LocalDateTime.now());
        SportsEventRegistration saved = regRepo.save(reg);

        // Registration process — the entry was not approved (optional reason).
        registrationEmailService.send(saved, RegistrationEmailService.Stage.REJECTED, reason);

        auditService.record(
            AuditAction.REGISTRATION_REJECTED,
            AuditModule.SPORTS,
            "SportsEventRegistration",
            String.valueOf(saved.getId()),
            "status=PENDING",
            "status=REJECTED, reason=" + reason
        );

        return saved;
    }

    @Override
    @Transactional
    public SportsEventRegistration nominateCaptain(Long registrationId, boolean nominate, String teamName) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
        
        SportsAuctionConfig config = auctionConfigRepo.findByEventId(reg.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionConfig for event", reg.getEvent().getId()));

        SportsAuctionTeam team = auctionTeamRepo.findByConfigIdAndOwnerUserId(config.getId(), reg.getUser().getId())
                .orElseGet(() -> SportsAuctionTeam.builder()
                        .config(config)
                        .community(config.getCommunity() != null ? config.getCommunity() : (reg.getEvent() != null ? reg.getEvent().getCommunity() : null))
                        .event(reg.getEvent())
                        .ownerUser(reg.getUser())
                        .captainUser(reg.getUser())
                        .ownerName(reg.getUser().getFullName())
                        .eventId(reg.getEvent().getId())
                        .totalBudget(config.getBudgetPerTeam())
                        .remainingBudget(config.getBudgetPerTeam())
                        .spent(0L)
                        .build());

        team.setCaptainNomination(nominate);
        if (nominate) {
            team.setTeamName(teamName);
        }
        
        SportsAuctionTeam savedTeam = auctionTeamRepo.save(team);
        hydrateCaptaincy(List.of(reg), reg.getEvent().getId());

        auditService.record(
            AuditAction.CAPTAIN_NOMINATED,
            AuditModule.SPORTS,
            "SportsAuctionTeam",
            String.valueOf(savedTeam.getId()),
            null,
            "teamName=" + savedTeam.getTeamName() + ", nominated=" + nominate
        );

        return reg;
    }

    @Override
    @Transactional
    public SportsEventRegistration confirmCaptain(Long registrationId, boolean confirm) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
        
        SportsAuctionConfig config = auctionConfigRepo.findByEventId(reg.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionConfig for event", reg.getEvent().getId()));

        SportsAuctionTeam team = auctionTeamRepo.findByConfigIdAndOwnerUserId(config.getId(), reg.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionTeam for user", reg.getUser().getId()));

        team.setCaptainConfirmation(confirm);
        SportsAuctionTeam savedTeam = auctionTeamRepo.save(team);
        
        hydrateCaptaincy(List.of(reg), reg.getEvent().getId());

        auditService.record(
            AuditAction.CAPTAIN_CONFIRMED,
            AuditModule.SPORTS,
            "SportsAuctionTeam",
            String.valueOf(savedTeam.getId()),
            "confirmed=" + !confirm,
            "confirmed=" + confirm
        );

        return reg;
    }

    @Override
    @Transactional
    public SportsEventRegistration respondToPartnerInvitation(Long registrationId, Long partnerUserId, boolean accept, String declineReason) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));

        if (reg.getPartner() == null || !reg.getPartner().getId().equals(partnerUserId)) {
            throw new UnauthorizedActionException("You are not the designated partner for this registration.");
        }

        if (reg.getStatus() == SportsEventRegistration.RegistrationStatus.WITHDRAWN ||
                reg.getStatus() == SportsEventRegistration.RegistrationStatus.REJECTED) {
            throw new InvalidInputException("Cannot respond to a " + reg.getStatus() + " registration.");
        }

        if (reg.getPartnerConfirmationStatus() != SportsEventRegistration.PartnerConfirmationStatus.PENDING) {
            throw new InvalidInputException("Partner invitation has already been " + reg.getPartnerConfirmationStatus() + ".");
        }

        if (accept) {
            reg.setPartnerConfirmationStatus(SportsEventRegistration.PartnerConfirmationStatus.CONFIRMED);
            reg.setPartnerConfirmedAt(LocalDateTime.now());
            reg.setPartnerDeclineReason(null);
        } else {
            reg.setPartnerConfirmationStatus(SportsEventRegistration.PartnerConfirmationStatus.DECLINED);
            reg.setPartnerConfirmedAt(LocalDateTime.now());
            reg.setPartnerDeclineReason(declineReason);
            reg.setStatus(SportsEventRegistration.RegistrationStatus.REJECTED);
            reg.setRejectReason("Partner declined invitation" + (declineReason != null && !declineReason.isBlank() ? ": " + declineReason : "."));
        }

        SportsEventRegistration saved = regRepo.save(reg);

        // Notify primary registering user
        if (reg.getUser() != null) {
            try {
                NotificationType type = accept ? NotificationType.PARTNER_CONFIRMED : NotificationType.PARTNER_DECLINED;
                String eventName = reg.getEvent() != null ? reg.getEvent().getName() : "Tournament";
                String partnerName = reg.getPartner() != null ? reg.getPartner().getFullName() : "Partner";
                String title = accept
                        ? "Doubles Partner Accepted — " + eventName
                        : "Doubles Partner Declined — " + eventName;
                String msg = accept
                        ? partnerName + " accepted your partner invitation for " + eventName + "."
                        : partnerName + " declined your partner invitation for " + eventName +
                          (declineReason != null && !declineReason.isBlank() ? ". Reason: " + declineReason : ".");

                notificationService.createNotification(
                        reg.getUser().getId(),
                        type,
                        NotificationCategory.EVENTS,
                        title,
                        msg,
                        null,
                        ReferenceType.SPORTS_EVENT,
                        reg.getEvent() != null ? reg.getEvent().getId() : null,
                        NotificationPriority.NORMAL,
                        null,
                        null
                );
            } catch (Exception e) {
                log.warn("Failed to send partner response notification", e);
            }
        }

        auditService.record(
                accept ? AuditAction.PARTNER_CONFIRMED : AuditAction.PARTNER_DECLINED,
                AuditModule.SPORTS,
                "SportsEventRegistration",
                String.valueOf(saved.getId()),
                "partnerConfirmationStatus=PENDING",
                "partnerConfirmationStatus=" + saved.getPartnerConfirmationStatus() + (declineReason != null ? ", reason=" + declineReason : "")
        );

        return saved;
    }

    @Override
    public List<SportsEventRegistration> getPartnerInvitations(Long partnerUserId, SportsEventRegistration.PartnerConfirmationStatus status) {
        if (status != null) {
            return regRepo.findByPartnerIdAndPartnerConfirmationStatus(partnerUserId, status);
        }
        return regRepo.findByPartnerId(partnerUserId);
    }

    private void hydrateCaptaincy(List<SportsEventRegistration> regs, Long eventId) {
        if (regs.isEmpty()) return;
        auctionConfigRepo.findByEventId(eventId).ifPresent(config -> {
            List<SportsAuctionTeam> teams = auctionTeamRepo.findByConfigIdOrderByTeamName(config.getId());
            for (SportsEventRegistration reg : regs) {
                teams.stream()
                    .filter(t -> t.getOwnerUser() != null && t.getOwnerUser().getId().equals(reg.getUser().getId()))
                    .findFirst()
                    .ifPresent(t -> {
                        reg.setCaptainNomination(t.getCaptainNomination());
                        reg.setCaptainConfirmation(t.getCaptainConfirmation());
                        reg.setProposedTeamName(t.getTeamName());
                    });
            }
        });
    }

    @Transactional
    public SportsEvent updateStatus(Long id, String status) {
        SportsEventStatus tournamentStatus;
        try {
            tournamentStatus = SportsEventStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ManaCommunityException(
                    "Invalid event status: '" + status + "'. Valid values: DRAFT, REGISTRATION_OPEN, "
                    + "REGISTRATION_CLOSED, LIVE, COMPLETED, CANCELLED.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }

        SportsEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        SportsTournament tournament = event.getTournament();
        if (tournament != null) {
            tournament.setRegistrationStatus(tournamentStatus);
            tournamentRepo.save(tournament);
        }

        boolean isClosing = tournamentStatus == SportsEventStatus.COMPLETED
                || tournamentStatus == SportsEventStatus.CANCELLED;

        if (isClosing && tournament != null && tournament.getSportsEvents() != null) {
            for (SportsEvent sibling : tournament.getSportsEvents()) {
                sibling.setActive(false);
                sibling.setStatus(tournamentStatus);
                sibling.setUpdatedAt(LocalDateTime.now());
                eventRepo.save(sibling);
            }
        } else if (tournament != null && tournament.getSportsEvents() != null) {
            for (SportsEvent sibling : tournament.getSportsEvents()) {
                sibling.setActive(true);
                sibling.setStatus(tournamentStatus);
                sibling.setUpdatedAt(LocalDateTime.now());
                eventRepo.save(sibling);
            }
        }

        event.setUpdatedAt(LocalDateTime.now());
        if (isClosing) {
            event.setActive(false);
        } else {
            event.setActive(true);
        }
        event.setStatus(tournamentStatus);
        SportsEvent saved = eventRepo.save(event);

        notifyEventParticipants(saved, tournamentStatus);

        return saved;
    }

    private LocalDateTime getTournamentStartDateTime(SportsEvent event) {
        if (event.getEventDateStart() == null) return null;
        java.time.LocalDate date = event.getEventDateStart();
        
        int hours = 9;
        int minutes = 0;
        
        String startTime = event.getStartTime();
        if (startTime != null && !startTime.trim().isEmpty()) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+):(\\d+)\\s*(AM|PM)?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(startTime);
            if (matcher.find()) {
                hours = Integer.parseInt(matcher.group(1));
                minutes = Integer.parseInt(matcher.group(2));
                String ampm = matcher.group(3);
                if (ampm != null) {
                    if ("PM".equalsIgnoreCase(ampm) && hours < 12) {
                        hours += 12;
                    } else if ("AM".equalsIgnoreCase(ampm) && hours == 12) {
                        hours = 0;
                    }
                }
            }
        }
        
        return date.atTime(hours, minutes);
    }

    private void scheduleNotifications(SportsEvent event, List<SportsNotificationScheduleDto> configs) {
        LocalDateTime eventStart = event.getEventDateStart().atTime(8, 0);
        LocalDateTime preciseStart = getTournamentStartDateTime(event);
        if (preciseStart == null) preciseStart = eventStart;

        for (SportsNotificationScheduleDto cfg : configs) {
            if (cfg.getId() == null && cfg.getOffsetType() != null) {
                // Legacy DTO shape (seeders / test mocks) — map onto the rich scheduler.
                LocalDateTime notifyAt = switch (cfg.getOffsetType()) {
                    case "DAYS"    -> eventStart.minusDays(cfg.getOffsetValue());
                    case "HOURS"   -> eventStart.minusHours(cfg.getOffsetValue());
                    case "MINUTES" -> eventStart.minusMinutes(cfg.getOffsetValue());
                    default        -> eventStart;
                };
                schedulerRepo.save(SportsNotificationScheduler.builder()
                        .event(event)
                        .community(event.getCommunity())
                        .triggerKey(cfg.getType())
                        .label(cfg.getType() != null ? cfg.getType() : "Reminder")
                        .offsetMinutes(0)
                        .enabled(true)
                        .title(cfg.getTitle() != null ? cfg.getTitle() : "Reminder")
                        .body(cfg.getBody() != null ? cfg.getBody() : "")
                        .recipients("Registered Players")
                        .channels("push,email")
                        .priority("NORMAL")
                        .isCustom(true)
                        .sent(false)
                        .notifyAt(notifyAt)
                        .build());
            } else {
                // Premium interactive multi-channel scheduler support
                int offsetMinutes = cfg.getOffset();
                LocalDateTime notifyAt = preciseStart.plusMinutes(offsetMinutes);
                
                String recipients = cfg.getRecipients() != null 
                        ? String.join(",", cfg.getRecipients()) 
                        : "Registered Players";
                String channels = cfg.getOverrideChannels() != null 
                        ? String.join(",", cfg.getOverrideChannels()) 
                        : "push,email";
                
                schedulerRepo.save(SportsNotificationScheduler.builder()
                        .event(event)
                        .community(event.getCommunity())
                        .triggerKey(cfg.getId())
                        .label(cfg.getLabel() != null ? cfg.getLabel() : "Custom Trigger")
                        .offsetMinutes(offsetMinutes)
                        .enabled(cfg.isEnabled())
                        .title(cfg.getTitle())
                        .body(cfg.getBody())
                        .recipients(recipients)
                        .channels(channels)
                        .priority(cfg.getPriority() != null ? cfg.getPriority().toUpperCase() : "NORMAL")
                        .isCustom(cfg.isCustom())
                        .sent(false)
                        .notifyAt(notifyAt)
                        .build());
            }
        }
    }

    public List<SportsEvent> getMyEvents(Long userId) {
        return eventRepo.findEventsForUser(userId);
    }

    public List<SportsEvent> getOpenEvents(Long communityId) {
        return eventRepo.findByCommunityIdAndTournamentRegistrationStatusInOrderByEventDateStartAsc(
                communityId,
                List.of(SportsEventStatus.REGISTRATION_OPEN));
    }

    @Override
    public List<SportsEvent> getAllOpenEvents() {
        return eventRepo.findByTournamentRegistrationStatusOrderByEventDateStartAsc(SportsEventStatus.REGISTRATION_OPEN);
    }

    @Override
    public List<SportsEvent> getClosedEvents() {
        List<SportsEvent> events = eventRepo.findByTournamentRegistrationStatusOrderByEventDateStartAsc(SportsEventStatus.REGISTRATION_CLOSED);
        for (SportsEvent event : events) {
            auctionConfigRepo.findByEventId(event.getId()).ifPresent(config -> {
                event.setAuctionStatus(SportsEvent.AuctionEventStatus.valueOf(config.getStatus().name()));
            });
        }
        return events;
    }

    @Override
    public List<SportsEvent> getClosedEvents(Long communityId) {
        List<SportsEvent> events = eventRepo.findByCommunityIdAndTournamentRegistrationStatusInOrderByEventDateStartAsc(
                communityId, List.of(SportsEventStatus.REGISTRATION_CLOSED));
        for (SportsEvent event : events) {
            auctionConfigRepo.findByEventId(event.getId()).ifPresent(config -> {
                event.setAuctionStatus(SportsEvent.AuctionEventStatus.valueOf(config.getStatus().name()));
            });
        }
        return events;
    }

    @Transactional
    public void syncTournaments() {
        List<SportsEvent> events = eventRepo.findAll();
        for (SportsEvent event : events) {
            boolean exists = tournamentRepo.existsByEventId(event.getId());
            if (!exists) {
                SportsTournament tournament = SportsTournament.builder()
                        .name(event.getName())
                        .sportsEvents(new java.util.ArrayList<>())
                        .community(event.getCommunity())
                        .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now())
                        .updatedAt(event.getUpdatedAt() != null ? event.getUpdatedAt() : LocalDateTime.now())
                        .build();
                event.setTournament(tournament);
                tournament.getSportsEvents().add(event);
                tournamentRepo.save(tournament);
            }
        }
    }

    @Override
    public List<SportsEvent> getAllEvents() {
        return eventRepo.findByActiveTrue();
    }

    @Override
    public Page<SportsEvent> getAllEvents(Pageable pageable) {
        return eventRepo.findByActiveTrue(pageable);
    }

    @Override
    public List<SportsEvent> getAllEventsIncludingInactive() {
        return eventRepo.findAll();
    }

    @Override
    public List<SportsEvent> getCommunityEvents(Long communityId) {
        return eventRepo.findByCommunityIdAndActiveTrueOrderByEventDateStartDesc(communityId);
    }

    @Override
    public List<SportsEvent> getCommunityEventsIncludingInactive(Long communityId) {
        return eventRepo.findByCommunityIdOrderByEventDateStartDesc(communityId);
    }

    @Override
    public Page<SportsEvent> getCommunityEvents(Long communityId, Pageable pageable) {
        return eventRepo.findByCommunityIdAndActiveTrue(communityId, pageable);
    }

    @Override
    public SportsEvent getEventById(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    @Override
    public SportsEvent getEventByUuid(java.util.UUID uuid) {
        return eventRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "uuid", String.valueOf(uuid)));
    }

    @Override
    public SportsEvent saveEvent(SportsEvent event) {
        return eventRepo.save(event);
    }

    @Override
    public List<java.util.Map<String, Object>> getEventMap(Long communityId) {
        List<SportsEvent> events;
        if (communityId == null) {
            events = eventRepo.findAll();
        } else {
            events = eventRepo.findByCommunityIdOrderByEventDateStartDesc(communityId);
        }
        return events.stream().map(e -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", e.getId());
            map.put("name", e.getName());
            return map;
        }).toList();
    }

    @Override
    public long getConfirmedRegistrationCount(Long eventId) {
        return regRepo.countByEventIdAndStatus(eventId, SportsEventRegistration.RegistrationStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        tournamentRepo.deleteByEventId(eventId);
        eventRepo.deleteById(eventId);

        auditService.record(
            AuditAction.SPORTS_EVENT_DELETED,
            AuditModule.SPORTS,
            "SportsEvent",
            String.valueOf(eventId),
            "id=" + eventId,
            null
        );
    }

    @Override
    @Transactional
    public SportsEvent updateEvent(Long eventId, SportsEventRequest req) {
        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        
        if (event.getTournament() != null) {
            validateEventDatesWithinTournament(req.getEventDateStart(), req.getEventDateEnd(), event.getTournament());
        }

        event.setName(req.getName());
        event.setEventDateStart(req.getEventDateStart());
        event.setEventDateEnd(req.getEventDateEnd());
        event.setRegistrationDateStart(req.getRegistrationDateStart());
        event.setRegistrationDateEnd(req.getRegistrationDateEnd());
        if (req.getVenueId() != null) {
            event.setVenue(venueRepo.findById(req.getVenueId()).orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId())));
        }
        event.setMaxParticipants(req.getMaxParticipants());

        if (req.getCategoryIds() != null) {
            event.setCategories(new java.util.HashSet<>(categoryRepo.findAllById(req.getCategoryIds())));
        }
        
        event.setDisputeCommittee(resolveDisputeCommittee(req.getDisputeCommitteeIds()));
        event.setMinPlayers(req.getMinPlayers());
        event.setMaxPlayers(req.getMaxPlayers());
        event.setGender(req.getGender());
        event.setPlayersBorn(req.getPlayersBorn());
        event.setFormat(parseMatchFormats(req.getFormat()));
        event.setTournamentType(req.getTournamentType() != null
                ? SportsEvent.TournamentType.valueOf(req.getTournamentType()) : null);
        
        event.setContactName(req.getContactName());
        event.setContactNumber(req.getContactNumber());
        event.setContactEmail(req.getContactEmail());
        event.getContacts().clear();
        event.getContacts().addAll(resolveContacts(req.getContacts()));
        event.setOtherContacts(req.getOtherContacts());
        if (req.getAuction() != null || req.getAuctionEnabled() != null) {
            Boolean isAuction = req.getAuction() != null ? req.getAuction() : req.getAuctionEnabled();
            event.setAuctionEnabled(isAuction);
        }
        event.setBannerImage(req.getBannerImage());
        event.setTournamentLevel(req.getTournamentLevel());
        event.setDescription(req.getDescription());
        event.setStartTime(req.getStartTime());
        event.setDueTime(req.getDueTime());
        if (req.getMinAge() != null) {
            event.setMinAge(req.getMinAge());
        }
        if (req.getMaxAge() != null) {
            event.setMaxAge(req.getMaxAge());
        }
        if (req.getAdminApprovalRequired() != null) {
            event.setAdminApprovalRequired(req.getAdminApprovalRequired());
        }
        if (req.getMandatoryMixedDoubles() != null) {
            event.setMandatoryMixedDoubles(req.getMandatoryMixedDoubles());
        }
        if (req.getAllowHigherAgeCategory() != null) {
            event.setAllowHigherAgeCategory(req.getAllowHigherAgeCategory());
        }
        if (req.getAllowMultipleCategories() != null) {
            event.setAllowMultipleCategories(req.getAllowMultipleCategories());
        }

        event.setUpdatedAt(LocalDateTime.now());
        
        if (event.getSponsors() != null) {
            event.getSponsors().clear();
        } else {
            event.setSponsors(new java.util.ArrayList<>());
        }
        if (req.getSponsors() != null) {
            for (SponsorDto s : req.getSponsors()) {
                event.getSponsors().add(SportsEventSponsor.builder()
                        .event(event)
                        .category(s.getCategory())
                        .name(s.getName())
                        .url(s.getUrl())
                        .build());
            }
        }
        
        SportsEvent saved = eventRepo.save(event);
        
        // Clean and reschedule notification rules on tournament update
        if (req.getNotifications() != null) {
            schedulerRepo.deleteByEventId(saved.getId());
            scheduleNotifications(saved, req.getNotifications());
        }

        auditService.record(
            AuditAction.SPORTS_EVENT_UPDATED,
            AuditModule.SPORTS,
            "SportsEvent",
            String.valueOf(saved.getId()),
            null,
            "name=" + saved.getName()
        );

        return saved;
    }



    private java.util.Set<AppUser> resolveDisputeCommittee(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new java.util.HashSet<>();
        return new java.util.HashSet<>(userRepo.findAllById(userIds));
    }

    @Override
    @Transactional
    public SportsEvent updateDisputeCommittee(Long eventId, List<Long> userIds) {
        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEvent", eventId));
        event.setDisputeCommittee(resolveDisputeCommittee(userIds));
        return eventRepo.save(event);
    }

    @Override
    @Transactional
    public SportsEventRegistration setRegistrationSeed(Long registrationId, Integer seed) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));
        reg.setSeed(seed);
        return regRepo.save(reg);
    }

     private void notifyEventParticipants(SportsEvent event, SportsEventStatus newStatus) {
        try {
            List<Long> userIds = regRepo.findByEventId(event.getId()).stream()
                    .filter(r -> r.getUser() != null && r.getUser().getId() != null)
                    .map(r -> r.getUser().getId())
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) return;

            NotificationType type;
            String title;
            String body;
            NotificationPriority priority;

            switch (newStatus) {
                case REGISTRATION_OPEN -> {
                    type = NotificationType.REGISTRATION_OPEN;
                    title = "Registrations Open — " + event.getName();
                    body = "Registrations are now open. Sign up before spots fill!";
                    priority = NotificationPriority.HIGH;
                }
                case CANCELLED -> {
                    type = NotificationType.EVENT_CANCELLED;
                    title = "Event Cancelled — " + event.getName();
                    body = "This event has been cancelled";
                    priority = NotificationPriority.HIGH;
                }
                case COMPLETED -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = "Event Completed — " + event.getName();
                    body = "This event has concluded. Thank you for participating!";
                    priority = NotificationPriority.NORMAL;
                }
                default -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = event.getName() + " — Status Update";
                    body = "Event status changed to " + newStatus.name().replace('_', ' ').toLowerCase();
                    priority = NotificationPriority.NORMAL;
                }
            }

            notificationService.createBulkNotifications(
                    userIds, type, NotificationCategory.EVENTS,
                    title, body, null,
                    ReferenceType.SPORTS_EVENT, event.getId(),
                    priority, null,
                    event.getCommunity() != null ? event.getCommunity().getId() : null);
        } catch (Exception e) {
            log.warn("Failed to persist event-status notifications for event {}", event.getId(), e);
        }
    }

    private String listToCommaString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private void validateEventDatesWithinTournament(LocalDate eventStart, LocalDate eventEnd, SportsTournament tournament) {
        if (tournament == null) return;
        LocalDate tourneyStart = tournament.getEventDateStart();
        LocalDate tourneyEnd = tournament.getEventDateEnd();

        if (tourneyStart != null) {
            if (eventStart != null && eventStart.isBefore(tourneyStart)) {
                throw new InvalidInputException("Event start date cannot be before tournament start date (" + tourneyStart + ")");
            }
            if (eventEnd != null && eventEnd.isBefore(tourneyStart)) {
                throw new InvalidInputException("Event end date cannot be before tournament start date (" + tourneyStart + ")");
            }
        }
        if (tourneyEnd != null) {
            if (eventStart != null && eventStart.isAfter(tourneyEnd)) {
                throw new InvalidInputException("Event start date cannot be after tournament end date (" + tourneyEnd + ")");
            }
            if (eventEnd != null && eventEnd.isAfter(tourneyEnd)) {
                throw new InvalidInputException("Event end date cannot be after tournament end date (" + tourneyEnd + ")");
            }
        }
    }

    private void validateAgeEligibility(int playerAge, SportsEvent event, SportsPlayerCategory category, String entityName) {
        // 1. Overall event age bounds
        int minAge = event.getMinAge() != null ? event.getMinAge() : 0;
        int maxAge = event.getMaxAge() != null ? event.getMaxAge() : 100;
        if (playerAge < minAge || playerAge > maxAge) {
            String sportName = event.getSport() != null ? event.getSport().getName() : event.getName();
            throw new AgeMismatchException(playerAge, minAge, maxAge, sportName + (entityName != null ? " (" + entityName + ")" : ""));
        }

        // 2. Category age bounds
        int catMinAge = category.getMinAge() != null ? category.getMinAge() : 0;
        int catMaxAge = category.getMaxAge() != null ? category.getMaxAge() : 100;

        if (category.getMinAge() == null && category.getMaxAge() == null && category.getName() != null) {
            String text = category.getName().toLowerCase();
            java.util.regex.Matcher rangeMatcher = java.util.regex.Pattern.compile("(?:between\\s*)?(\\d+)\\s*(?:-|–|to)\\s*(\\d+)").matcher(text);
            java.util.regex.Matcher underMatcher = java.util.regex.Pattern.compile("(?:under|u-?|below|<|<=)\\s*(\\d+)").matcher(text);
            java.util.regex.Matcher plusMatcher = java.util.regex.Pattern.compile("(?:above|over|>|>=)\\s*(\\d+)|(\\d+)\\s*(?:\\+|plus|above|and above|and over|over|>|>=)").matcher(text);

            if (rangeMatcher.find()) {
                catMinAge = Integer.parseInt(rangeMatcher.group(1));
                catMaxAge = Integer.parseInt(rangeMatcher.group(2));
            } else if (underMatcher.find()) {
                catMaxAge = Integer.parseInt(underMatcher.group(1));
            } else if (plusMatcher.find()) {
                String g = plusMatcher.group(1) != null ? plusMatcher.group(1) : plusMatcher.group(2);
                catMinAge = Integer.parseInt(g);
            } else if (text.matches(".*\\b(kids?|childrens?)\\b.*")) {
                catMaxAge = 16;
            } else if (text.matches(".*\\b(seniors?|veterans?)\\b.*")) {
                catMinAge = 45;
            }
        }

        boolean allowHigher = event.getAllowHigherAgeCategory() == null || event.getAllowHigherAgeCategory();

        boolean isSeniorCategory = "SENIORS".equalsIgnoreCase(category.getCategory_type())
                || (category.getName() != null && (category.getName().toLowerCase().contains("senior")
                || category.getName().toLowerCase().contains("master")
                || category.getName().contains("40+")
                || category.getName().contains("50+")
                || category.getName().contains("60+")));

        if (allowHigher) {
            // Higher Age Category mode (playing up):
            // a) Player cannot play down into a younger category:
            if (playerAge > catMaxAge) {
                throw new AgeMismatchException(playerAge, catMinAge, catMaxAge, category.getName() + (entityName != null ? " (" + entityName + ")" : ""));
            }
            // b) Senior / Masters categories strictly enforce the minimum age requirement:
            if (isSeniorCategory && playerAge < catMinAge) {
                throw new AgeMismatchException(playerAge, catMinAge, catMaxAge, category.getName() + (entityName != null ? " (" + entityName + ")" : ""));
            }
        } else {
            // Strict category bounds
            if (playerAge < catMinAge || playerAge > catMaxAge) {
                throw new AgeMismatchException(playerAge, catMinAge, catMaxAge, category.getName() + (entityName != null ? " (" + entityName + ")" : ""));
            }
        }
    }

    private void validateOptionBDuplicates(
            AppUser user,
            com.manacommunity.api.user.model.FamilyMember familyMember,
            String participantName,
            SportsEvent currentEvent,
            SportsPlayerCategory currentCategory,
            SportsEvent.MatchFormat currentFormat,
            List<SportsEventRegistration.RegistrationStatus> activeStatuses
    ) {
        if (currentEvent == null || currentEvent.getSport() == null || user == null) return;
        Long sportId = currentEvent.getSport().getId();
        Long tourneyId = currentEvent.getTournament() != null ? currentEvent.getTournament().getId() : null;
        boolean allowMultiCats = currentEvent.getAllowMultipleCategories() == null || currentEvent.getAllowMultipleCategories();

        List<SportsEventRegistration> existingRegs = regRepo.findByUserId(user.getId()).stream()
                .filter(r -> {
                    if (familyMember != null) {
                        return r.getFamilyMember() != null && r.getFamilyMember().getId().equals(familyMember.getId());
                    } else {
                        return r.getFamilyMember() == null;
                    }
                })
                .toList();

        for (SportsEventRegistration r : existingRegs) {
            if (r.getEvent() == null || r.getEvent().getId() == null || currentEvent.getId() == null || r.getEvent().getId().equals(currentEvent.getId())) {
                continue;
            }
            if (r.getStatus() == null || !activeStatuses.contains(r.getStatus())) {
                continue;
            }
            SportsEvent otherEvent = r.getEvent();
            if (otherEvent.getSport() == null || !otherEvent.getSport().getId().equals(sportId)) {
                continue;
            }

            boolean inSameTournament = tourneyId != null && otherEvent.getTournament() != null && tourneyId.equals(otherEvent.getTournament().getId());
            boolean hasDateOverlap = datesOverlap(currentEvent, otherEvent);

            if (inSameTournament || hasDateOverlap) {
                String otherCatName = r.getCategory() != null ? r.getCategory().getName() : "another category";
                String sportName = currentEvent.getSport().getName();

                if (!allowMultiCats) {
                    throw new AlreadyRegisteredException(
                            participantName + " is already registered for " + sportName + " in '" + otherEvent.getName()
                            + "' (" + otherCatName + "). Multiple event registrations for the same sport are not permitted."
                    );
                }

                // Option B: Format-specific single entry (e.g. at most 1 Singles, 1 Doubles)
                if (r.getMatchType() != null && currentFormat != null && r.getMatchType() == currentFormat) {
                    throw new AlreadyRegisteredException(
                            participantName + " is already registered for " + sportName + " " + currentFormat.name().replace('_', ' ')
                            + " in '" + otherEvent.getName() + "' (" + otherCatName + "). A player cannot enter multiple age categories of the same match format."
                    );
                }
            }
        }
    }

    private boolean datesOverlap(SportsEvent e1, SportsEvent e2) {
        LocalDate s1 = e1.getEventDateStart();
        LocalDate eDate1 = e1.getEventDateEnd() != null ? e1.getEventDateEnd() : s1;
        LocalDate s2 = e2.getEventDateStart();
        LocalDate eDate2 = e2.getEventDateEnd() != null ? e2.getEventDateEnd() : s2;
        if (s1 == null || s2 == null) return false;
        return !eDate1.isBefore(s2) && !s1.isAfter(eDate2);
    }
}
