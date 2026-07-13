package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.TournamentRepository;
import com.manacommunity.api.repository.SportsEventRepository;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.ContactRepository;
import com.manacommunity.api.dto.ContactDto;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepo;
    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final CommunityRepository communityRepo;
    private final ContactRepository contactRepository;
    private final NotificationManagementService notificationService;

    private List<Contact> resolveContacts(List<ContactDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
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
        }).collect(Collectors.toList());
    }

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
        Tournament tournament = tournamentRepo.findById(id).orElse(null);
        if (tournament == null) {
            return; // already gone — treat delete as idempotent
        }

        // Disassociate the sports events instead of deleting them: null out the
        // FK on each event so the tournament can be removed without cascade-
        // deleting events (which own registrations/matches and would block the
        // delete). The events survive, just unlinked from any tournament.
        List<SportsEvent> events = eventRepo.findByTournamentId(id);
        for (SportsEvent ev : events) {
            ev.setTournament(null);
        }
        eventRepo.saveAll(events);

        // Keep the in-memory collection consistent so JPA doesn't re-link on flush.
        if (tournament.getSportsEvents() != null) {
            tournament.getSportsEvents().clear();
        }

        tournamentRepo.delete(tournament);
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
        tournament.getContacts().clear();
        tournament.getContacts().addAll(resolveContacts(req.getContacts()));
        tournament.setOtherContacts(req.getOtherContacts());
        
        tournament.setBannerImage(req.getBannerImage());
        tournament.setDescription(req.getDescription());
        tournament.setAllowAdminChat(allowAdminChat);
        
        tournament.setStartTime(req.getStartTime());
        tournament.setDueTime(req.getDueTime());

        if (req.getCommunityId() != null) {
            tournament.setCommunity(communityRepo.findById(req.getCommunityId()).orElse(null));
        }

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
        tournament.setRegistrationStatus(Tournament.EventStatus.DRAFT);

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
            SportsEvent event = eventRepo.findById(id)
                    .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException(
                            "Tournament or Event", id));
            tournament = event.getTournament();
            if (tournament == null) {
                throw new com.manacommunity.api.exception.ManaCommunityException(
                        "Event " + id + " is not linked to a tournament.",
                        org.springframework.http.HttpStatus.BAD_REQUEST, "NO_TOURNAMENT");
            }
        }

        tournament.setRegistrationStatus(tournamentStatus);
        Tournament saved = tournamentRepo.save(tournament);

        if (tournamentStatus == Tournament.EventStatus.COMPLETED
                || tournamentStatus == Tournament.EventStatus.CANCELLED) {
            if (saved.getSportsEvents() != null) {
                for (SportsEvent ev : saved.getSportsEvents()) {
                    ev.setActive(false);
                    ev.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
                    ev.setUpdatedAt(java.time.LocalDateTime.now());
                    eventRepo.save(ev);
                }
            }
        } else {
            if (saved.getSportsEvents() != null) {
                for (SportsEvent ev : saved.getSportsEvents()) {
                    ev.setActive(true);
                    ev.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
                    ev.setUpdatedAt(java.time.LocalDateTime.now());
                    eventRepo.save(ev);
                }
            }
        }

        notifyTournamentParticipants(saved, tournamentStatus);

        return saved;
    }

    private void notifyTournamentParticipants(Tournament tournament, Tournament.EventStatus newStatus) {
        try {
            if (tournament.getSportsEvents() == null || tournament.getSportsEvents().isEmpty()) return;

            List<Long> userIds = tournament.getSportsEvents().stream()
                    .flatMap(ev -> regRepo.findByEventId(ev.getId()).stream())
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
                    title = "Registrations Open — " + tournament.getName();
                    body = "Registrations are now open for " + tournament.getName();
                    priority = NotificationPriority.HIGH;
                }
                case CANCELLED -> {
                    type = NotificationType.EVENT_CANCELLED;
                    title = "Tournament Cancelled — " + tournament.getName();
                    body = "This tournament has been cancelled";
                    priority = NotificationPriority.HIGH;
                }
                case COMPLETED -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = "Tournament Completed — " + tournament.getName();
                    body = "The tournament has concluded. Thank you for participating!";
                    priority = NotificationPriority.NORMAL;
                }
                default -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = tournament.getName() + " — Status Update";
                    body = "Status changed to " + newStatus.name().replace('_', ' ').toLowerCase();
                    priority = NotificationPriority.NORMAL;
                }
            }

            notificationService.createBulkNotifications(
                    userIds, type, NotificationCategory.EVENTS,
                    title, body, null,
                    ReferenceType.TOURNAMENT, tournament.getId(),
                    priority, null,
                    tournament.getCommunity() != null ? tournament.getCommunity().getId() : null);
        } catch (Exception e) {
            log.warn("Failed to persist tournament-status notifications for tournament {}: {}",
                    tournament.getId(), e.getMessage());
        }
    }
}
