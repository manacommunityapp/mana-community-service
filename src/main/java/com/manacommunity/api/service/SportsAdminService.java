package com.manacommunity.api.service;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.dto.dashboard.SportsAdminFormDataResponse;
import com.manacommunity.api.dto.dashboard.SportsAdminFormDataResponse.CategoryOption;
import com.manacommunity.api.dto.dashboard.SportsAdminFormDataResponse.SportOption;
import com.manacommunity.api.dto.dashboard.SportsAdminOverviewResponse;
import com.manacommunity.api.dto.dashboard.SportsAdminOverviewResponse.EventRow;
import com.manacommunity.api.dto.dashboard.SportsAdminOverviewResponse.SportRef;
import com.manacommunity.api.dto.dashboard.SportsAdminOverviewResponse.TournamentRow;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.PlayerCategory;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.Tournament;
import com.manacommunity.api.repository.PlayerCategoryRepository;
import com.manacommunity.api.repository.SportMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    private final TournamentService tournamentService;
    private final SportsEventService eventService;
    private final CommunityService communityService;
    private final SportMetaRepository sportMetaRepo;
    private final PlayerCategoryRepository categoryRepo;

    // ── Overview (Dashboard / Sports Event tabs) ──────────────────────

    @Transactional(readOnly = true)
    public SportsAdminOverviewResponse getOverview(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<Tournament> tournaments = isSuperAdmin
                ? tournamentService.getAllTournaments()
                : (communityId != null ? tournamentService.getCommunityTournaments(communityId) : List.of());

        PageRequest eventPage = PageRequest.of(0, 100, Sort.by("eventDateStart").descending());
        List<SportsEvent> events = isSuperAdmin
                ? eventService.getAllEvents(eventPage).getContent()
                : (communityId != null ? eventService.getCommunityEvents(communityId, eventPage).getContent() : List.of());

        List<TournamentRow> tournamentRows = tournaments.stream()
                .map(this::toTournamentRow)
                .toList();

        List<EventRow> eventRows = events.stream()
                .map(this::toEventRow)
                .toList();

        return new SportsAdminOverviewResponse(tournamentRows, eventRows);
    }

    private TournamentRow toTournamentRow(Tournament t) {
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
        Tournament t = e.getTournament();
        return new EventRow(
                e.getId(),
                e.getName(),
                e.getEventDateStart(),
                e.getEventDateEnd(),
                e.getFormat(),
                e.getTournamentType() != null ? e.getTournamentType().name() : null,
                e.getGender(),
                e.getMinAge(),
                e.getMaxAge(),
                e.getMaxParticipants(),
                t != null && t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null,
                e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null,
                sport != null ? new SportRef(sport.getName(), sport.getIcon(), sport.getIconUrl()) : null,
                t != null ? t.getId() : null
        );
    }

    // ── Form data (Create Tournament / Create Venue tabs) ─────────────

    @Transactional(readOnly = true)
    public SportsAdminFormDataResponse getFormData(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<SportOption> sports = sportMetaRepo.findByActiveTrue().stream()
                .map(s -> new SportOption(
                        s.getId(), s.getName(), s.getIcon(), s.getIconUrl(),
                        s.getFormats(), s.getCommunityId()))
                .toList();

        // Same visibility rule as SportsController#getCategories.
        List<PlayerCategory> rawCategories = (isSuperAdmin || communityId == null)
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
