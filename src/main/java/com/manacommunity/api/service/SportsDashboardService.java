package com.manacommunity.api.service;

import com.manacommunity.api.model.Community;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.dto.dashboard.SportsDashboardResponse;
import com.manacommunity.api.dto.dashboard.SportsDashboardResponse.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.PlayerCategory;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.model.Tournament;
import com.manacommunity.api.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assembles the Sports Dashboard in a single read-only pass, reusing the existing
 * {@link SportsEventService} reads and projecting each entity down to just the
 * fields the page renders. Runs in one transaction so the lazy associations
 * (sport / venue / category / tournament) are mapped while the session is open.
 */
@Service
@RequiredArgsConstructor
public class SportsDashboardService {

    private final SportsEventService eventService;
    private final TournamentRepository tournamentRepo;

    @Transactional(readOnly = true)
    public SportsDashboardResponse getDashboard(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<SportsEvent> openEvents = isSuperAdmin
                ? eventService.getAllOpenEvents()
                : (communityId != null ? eventService.getOpenEvents(communityId) : List.of());

        List<SportsEvent> closedEvents = isSuperAdmin
                ? eventService.getClosedEvents()
                : (communityId != null ? eventService.getClosedEvents(communityId) : List.of());

        List<SportsEvent> myEvents = eventService.getMyEvents(user.getId());
        List<SportsEventRegistration> myRegs = eventService.getUserRegistrations(user.getId());

        // Index the user's registrations by event id so open-event cards can show the right button.
        Map<Long, SportsEventRegistration> regByEvent = myRegs.stream()
                .filter(r -> r.getEvent() != null && r.getEvent().getId() != null)
                .collect(Collectors.toMap(
                        r -> r.getEvent().getId(),
                        Function.identity(),
                        (a, b) -> a)); // keep first on duplicate

        List<EventCard> open = openEvents.stream()
                .map(e -> toEventCard(e, regByEvent.get(e.getId())))
                .toList();

        List<EventCard> closed = closedEvents.stream()
                .map(e -> toEventCard(e, regByEvent.get(e.getId())))
                .toList();

        List<UpcomingEvent> upcoming = myEvents.stream()
                .map(this::toUpcomingEvent)
                .toList();

        List<MyRegistration> registrations = myRegs.stream()
                .map(this::toMyRegistration)
                .toList();

        int liveCount = (int) openEvents.stream()
                .filter(e -> "LIVE".equals(registrationStatus(e)))
                .count();

        Stats stats = new Stats(
                myEvents.size(),     // Your Registrations
                liveCount,           // Live Events
                openEvents.size(),   // Open Registrations
                0                    // Community Players — placeholder, matches current UI
        );

        // Query tournaments directly from the tournament table for the grouped view.
        List<Tournament> openTournamentEntities = isSuperAdmin
                ? tournamentRepo.findByRegistrationStatusWithEvents(Tournament.EventStatus.REGISTRATION_OPEN)
                : (communityId != null
                    ? tournamentRepo.findByRegistrationStatusAndCommunityWithEvents(Tournament.EventStatus.REGISTRATION_OPEN, communityId)
                    : List.of());
        List<TournamentCard> openTournaments = buildTournamentCards(openTournamentEntities, regByEvent);

        return new SportsDashboardResponse(stats, open, closed, upcoming, registrations, openTournaments);
    }

    // ── Tournament grouping ────────────────────────────────────────────

    private List<TournamentCard> buildTournamentCards(
            List<Tournament> tournaments,
            Map<Long, SportsEventRegistration> regByEvent) {

        List<TournamentCard> cards = new ArrayList<>();
        for (Tournament t : tournaments) {
            List<SportsEvent> childEvents = t.getSportsEvents() != null ? t.getSportsEvents() : List.of();
            List<EventCard> eventCards = childEvents.stream()
                    .filter(e -> e.getActive() == null || e.getActive())
                    .map(e -> toEventCard(e, regByEvent.get(e.getId())))
                    .toList();
            cards.add(new TournamentCard(
                    t.getId(),
                    t.getName(),
                    t.getBannerImage(),
                    t.getEventDateStart(),
                    t.getEventDateEnd(),
                    t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null,
                    t.getCommunity() != null ? t.getCommunity().getId() : null,
                    t.getCommunity() != null ? t.getCommunity().getName() : null,
                    eventCards
            ));
        }
        return cards;
    }

    // ── Mapping helpers ───────────────────────────────────────────────

    private EventCard toEventCard(SportsEvent e, SportsEventRegistration myReg) {
        return new EventCard(
                e.getId(),
                e.getUuid(),
                e.getName(),
                e.getEventDateStart(),
                e.getEventDateEnd(),
                e.getSport() != null ? e.getSport().getName() : null,
                firstCategoryName(e),
                e.getVenue() != null ? e.getVenue().getName() : null,
                e.getMaxParticipants(),
                registrationStatus(e),
                e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null,
                isTeamSport(e),
                myReg != null ? myReg.getId() : null,
                myReg != null && myReg.getStatus() != null ? myReg.getStatus().name() : null
        );
    }

    private UpcomingEvent toUpcomingEvent(SportsEvent e) {
        return new UpcomingEvent(
                e.getId(),
                e.getName(),
                e.getSport() != null ? e.getSport().getName() : null,
                e.getVenue() != null ? e.getVenue().getName() : null,
                firstCategoryName(e),
                registrationStatus(e),
                e.getEventDateStart(),
                e.getStartTime()
        );
    }

    private MyRegistration toMyRegistration(SportsEventRegistration r) {
        SportsEvent e = r.getEvent();
        return new MyRegistration(
                r.getId(),
                e != null ? e.getId() : null,
                e != null ? e.getName() : null,
                e != null ? e.getEventDateStart() : null,
                e != null && e.getSport() != null ? e.getSport().getName() : null,
                r.getCategory() != null ? r.getCategory().getName() : null,
                e != null ? registrationStatus(e) : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getMatchType() != null ? r.getMatchType().name() : null,
                r.getCaptainNomination(),
                r.getCaptainConfirmation()
        );
    }

    /** Event status lives on the linked tournament (REGISTRATION_OPEN / CLOSED / LIVE …). */
    private String registrationStatus(SportsEvent e) {
        Tournament t = e.getTournament();
        if (t != null && t.getRegistrationStatus() != null) {
            return t.getRegistrationStatus().name();
        }
        return null;
    }

    private boolean isTeamSport(SportsEvent e) {
        List<String> format = e.getFormat();
        return format != null && format.contains("TEAM");
    }

    private String firstCategoryName(SportsEvent e) {
        if (e.getCategories() == null || e.getCategories().isEmpty()) return null;
        return e.getCategories().stream()
                .filter(Objects::nonNull)
                .map(PlayerCategory::getName)
                .findFirst()
                .orElse(null);
    }
}
