package com.manacommunity.api.sports.service;
import com.manacommunity.api.sports.dto.SportsAdminOverviewResponse.*;
import com.manacommunity.api.sports.dto.SportsAdminFormDataResponse.*;
import com.manacommunity.api.service.CommunityService;

import com.manacommunity.api.sports.controller.SportsController;
import com.manacommunity.api.sports.model.SportsVenue;


import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.sports.dto.SportsAdminFormDataResponse;
import com.manacommunity.api.sports.dto.SportsAdminOverviewResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.sports.model.SportsPlayerCategory;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsMeta;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.repository.SportsPlayerCategoryRepository;
import com.manacommunity.api.sports.repository.SportsMetaRepository;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.sports.model.SportsEventRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates the Sports Admin reads into lean, purpose-built payloads — the list
 * tabs and the create forms each get exactly the fields they bind to, in one call,
 * instead of several full-entity fetches. Mirrors the scoping rules of
 * {@code SportsController} (SUPER_ADMIN sees all; others see their community).
 */
@Service
@RequiredArgsConstructor
public class SportsAdminService {

    private final SportsTournamentService tournamentService;
    private final SportsEventService eventService;
    private final CommunityService communityService;
    private final SportsMetaRepository sportMetaRepo;
    private final SportsPlayerCategoryRepository categoryRepo;
    private final SportsEventRegistrationRepository regRepo;

    // ── Overview (Dashboard / Sports Event tabs) ──────────────────────

    @Transactional(readOnly = true)
    public SportsAdminOverviewResponse getOverview(AppUser user, Long communityId) {
        boolean isSuperAdmin = user.hasRole(ROLE_SUPER_ADMIN);
        Long activeCommId = isSuperAdmin ? communityId : (user.getCommunity() != null ? user.getCommunity().getId() : null);

        List<SportsTournament> tournaments = isSuperAdmin && activeCommId == null
                ? tournamentService.getAllTournaments()
                : (activeCommId != null ? tournamentService.getCommunityTournaments(activeCommId) : List.of());

        List<SportsEvent> events = loadAllEvents(isSuperAdmin && activeCommId == null, activeCommId);

        List<TournamentRow> tournamentRows = tournaments.stream()
                .map(this::toTournamentRow)
                .toList();

        List<EventRow> eventRows = events.stream()
                .map(this::toEventRow)
                .toList();

        List<SportsEventRegistration> registrations;
        if (isSuperAdmin && activeCommId == null) {
            registrations = regRepo.findAll();
        } else if (activeCommId != null) {
            registrations = regRepo.findByCommunityId(activeCommId);
        } else {
            registrations = List.of();
        }

        List<RegistrationRow> pendingRegistrations = registrations.stream()
                .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.PENDING 
                        || r.getStatus() == SportsEventRegistration.RegistrationStatus.REGISTERED)
                .map(this::toRegistrationRow)
                .toList();

        List<RegistrationRow> confirmedRegistrations = registrations.stream()
                .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.CONFIRMED)
                .map(this::toRegistrationRow)
                .toList();

        List<RegistrationRow> allRegistrations = registrations.stream()
                .map(this::toRegistrationRow)
                .toList();

        return new SportsAdminOverviewResponse(tournamentRows, eventRows, pendingRegistrations, confirmedRegistrations, allRegistrations);
    }

    private RegistrationRow toRegistrationRow(SportsEventRegistration r) {
        var familyMember = r.getFamilyMember();
        var user = r.getUser();
        var category = r.getCategory();
        String eventName = r.getEvent() != null ? r.getEvent().getName() : "";
        String sportName = r.getEvent() != null && r.getEvent().getSport() != null ? r.getEvent().getSport().getName() : "";
        String participantName = r.getPlayerName();
        if (participantName == null || participantName.isBlank()) {
            participantName = familyMember != null ? familyMember.getName() : user != null ? user.getFullName() : null;
        }
        String participantGender = familyMember != null ? familyMember.getGender() : user != null ? user.getGender() : null;
        Integer participantAge = r.getAge() != null ? r.getAge() : familyMember != null ? familyMember.getAge() : null;
        return new RegistrationRow(
                r.getId(),
                r.getEvent() != null ? r.getEvent().getId() : null,
                user != null ? user.getId() : null,
                familyMember != null ? familyMember.getId() : null,
                participantName,
                participantGender,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                category != null ? category.getGender() : null,
                r.getPlayerName(),
                r.getEmail(),
                r.getFlatNumber(),
                r.getRole(),
                r.getRelation(),
                participantAge,
                r.getStatus() != null ? r.getStatus().name() : null,
                eventName,
                sportName,
                r.getRegisteredAt(),
                r.getProposedTeamName()
        );
    }

    private List<SportsEvent> loadAllEvents(boolean allCommunities, Long communityId) {
        if (!allCommunities && communityId == null) return List.of();
        List<SportsEvent> result = new java.util.ArrayList<>();
        int pageNumber = 0;
        Page<SportsEvent> page;
        do {
            PageRequest pageable = PageRequest.of(pageNumber++, 100, Sort.by("eventDateStart").descending());
            page = allCommunities
                    ? eventService.getAllEvents(pageable)
                    : eventService.getCommunityEvents(communityId, pageable);
            result.addAll(page.getContent());
        } while (page.hasNext());
        return result;
    }

    private TournamentRow toTournamentRow(SportsTournament t) {
        List<EventRow> nested = t.getSportsEvents() == null ? List.of()
                : t.getSportsEvents().stream().map(this::toEventRow).toList();
        return new TournamentRow(
                t.getId(),
                t.getName(),
                t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null,
                t.getMaxParticipants(),
                t.getEventDateStart(),
                t.getEventDateEnd(),
                nested
        );
    }

    private EventRow toEventRow(SportsEvent e) {
        SportsMeta sport = e.getSport();
        SportsTournament t = e.getTournament();
        return new EventRow(
                e.getId(),
                e.getName(),
                e.getEventDateStart(),
                e.getEventDateEnd(),
                e.getFormat() != null ? e.getFormat().stream().map(Enum::name).toList() : java.util.List.of(),
                e.getTournamentType() != null ? e.getTournamentType().name() : null,
                e.getGender(),
                e.getMinAge(),
                e.getMaxAge(),
                e.getMaxParticipants(),
                t != null && t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null,
                e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null,
                sport != null ? new SportRef(sport.getName(), sport.getIcon(), sport.getIconUrl()) : null,
                t != null ? t.getId() : null,
                e.getAdminApprovalRequired(),
                e.getVenue() != null ? e.getVenue().getName() : null,
                e.getVenue() != null ? e.getVenue().getCity() : null
        );
    }

    // ── Form data (Create SportsTournament / Create Venue tabs) ─────────────

    @Transactional(readOnly = true)
    public SportsAdminFormDataResponse getFormData(AppUser user) {
        boolean isSuperAdmin = user.hasRole(ROLE_SUPER_ADMIN);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<SportOption> sports = sportMetaRepo.findByActiveTrue().stream()
                .map(s -> new SportOption(
                        s.getId(), s.getName(), s.getIcon(), s.getIconUrl(),
                        s.getFormats(), s.getCommunityId()))
                .toList();

        // Same visibility rule as SportsController#getCategories.
        List<SportsPlayerCategory> rawCategories = (isSuperAdmin || communityId == null)
                ? categoryRepo.findAll()
                : categoryRepo.findDefaultAndCommunityCategories(communityId);

        List<CategoryOption> categories = rawCategories.stream()
                .map(c -> new CategoryOption(
                        c.getId(), c.getName(), c.getType(), c.getCategory_type(),
                        c.getDescription(), c.getMinAge(), c.getMaxAge(), c.getGender()))
                .toList();

        return new SportsAdminFormDataResponse(
                sports, categories, communityService.getAllCommunities());
    }
}
