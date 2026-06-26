package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.Tournament;
import com.manacommunity.api.model.SportsEventSponsor;
import com.manacommunity.api.repository.TournamentRepository;
import com.manacommunity.api.repository.SportsEventRepository;
import com.manacommunity.api.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepo;
    private final SportsEventRepository eventRepo;

    @Override
    public List<Tournament> getAllTournaments() {
        return tournamentRepo.findAll();
    }

    @Override
    public List<Tournament> getCommunityTournaments(Long communityId) {
        return tournamentRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @Override
    public Tournament getTournamentById(Long id) {
        return tournamentRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Tournament", id));
    }

    @Override
    @Transactional
    public void deleteTournament(Long id) {
        tournamentRepo.deleteById(id);
    }

    @Override
    @Transactional
    public Tournament saveTournamentRecord(TournamentRequest req, Boolean allowAdminChat) {
        // Resolve an existing Tournament if it is linked to any of the selected event IDs
        Tournament tournament = null;
        if (req.getSportsEventIds() != null && !req.getSportsEventIds().isEmpty()) {
            for (Long eventId : req.getSportsEventIds()) {
                Optional<Tournament> existing = tournamentRepo.findByEventId(eventId);
                if (existing.isPresent()) {
                    tournament = existing.get();
                    break;
                }
            }
        }
        if (tournament == null) {
            tournament = new Tournament();
        }

        // Map tournament fields directly from TournamentRequest DTO
        tournament.setName(req.getName());
        tournament.setEventDateStart(req.getEventDateStart());
        tournament.setEventDateEnd(req.getEventDateEnd());
        tournament.setRegistrationDateStart(req.getRegistrationDateStart());
        tournament.setRegistrationDateEnd(req.getRegistrationDateEnd());
        tournament.setMaxParticipants(req.getMaxParticipants());
        
        tournament.setContactName(req.getContactName());
        tournament.setContactNumber(req.getContactNumber());
        tournament.setContactEmail(req.getContactEmail());
        tournament.setOtherContacts(req.getOtherContacts());
        
        tournament.setBannerImage(req.getBannerImage());
        tournament.setDescription(req.getDescription());
        tournament.setAllowAdminChat(allowAdminChat);
        
        tournament.setStartTime(req.getStartTime());
        tournament.setDueTime(req.getDueTime());

        // Resolve existing SportsEvents from req.getSportsEventIds()
        List<SportsEvent> sportsEvents = new ArrayList<>();
        if (req.getSportsEventIds() != null && !req.getSportsEventIds().isEmpty()) {
            sportsEvents = eventRepo.findAllById(req.getSportsEventIds());
        }

        // Unlink events that are no longer associated
        if (tournament.getSportsEvents() != null) {
            for (SportsEvent oldEvent : new ArrayList<>(tournament.getSportsEvents())) {
                if (!sportsEvents.contains(oldEvent)) {
                    oldEvent.setTournament(null);
                    eventRepo.save(oldEvent);
                }
            }
        }

        // Link new/current events
        tournament.setSportsEvents(new ArrayList<>());
        for (SportsEvent ev : sportsEvents) {
            ev.setTournament(tournament);
            tournament.getSportsEvents().add(ev);
            eventRepo.save(ev);
        }

        // Build mainEvent context for sponsors mapping
        SportsEvent mainEvent = null;
        if (!sportsEvents.isEmpty()) {
            mainEvent = sportsEvents.get(0);
        }

        // Clear and rebuild sponsors directly from request DTO
        if (tournament.getSponsors() != null) {
            tournament.getSponsors().clear();
        } else {
            tournament.setSponsors(new ArrayList<>());
        }

        if (req.getSponsors() != null) {
            for (com.manacommunity.api.dto.SponsorDto s : req.getSponsors()) {
                tournament.getSponsors().add(SportsEventSponsor.builder()
                        .tournament(tournament)
                        .event(mainEvent)
                        .category(s.getCategory())
                        .name(s.getName())
                        .url(s.getUrl())
                        .build());
            }
        }

        if (tournament.getCreatedAt() == null) {
            tournament.setCreatedAt(java.time.LocalDateTime.now());
        }
        tournament.setUpdatedAt(java.time.LocalDateTime.now());

        return tournamentRepo.save(tournament);
    }

    @Override
    @Transactional
    public Tournament updateStatus(Long id, String status) {
        Tournament.EventStatus tournamentStatus;
        try {
            tournamentStatus = Tournament.EventStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new com.manacommunity.api.exception.ManaCommunityException(
                    "Invalid event status: '" + status + "'. Valid values: DRAFT, REGISTRATION_OPEN, "
                    + "REGISTRATION_CLOSED, LIVE, COMPLETED, CANCELLED.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }

        Tournament tournament = tournamentRepo.findById(id).orElse(null);
        if (tournament == null) {
            SportsEvent event = eventRepo.findById(id).orElse(null);
            if (event != null) {
                tournament = event.getTournament();
            }
        }

        if (tournament == null) {
            throw new com.manacommunity.api.exception.ResourceNotFoundException("Tournament or Event", id);
        }

        tournament.setRegistrationStatus(tournamentStatus);
        return tournamentRepo.save(tournament);
    }
}
