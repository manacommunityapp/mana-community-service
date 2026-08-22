package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.Competition;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.service.CompetitionService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository repository;
    private final CommunityEventRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;

    public CompetitionServiceImpl(CompetitionRepository repository,
                                  CommunityEventRepository eventRepository,
                                  EventBookingRegistrationRepository bookingRepo) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Competition> getAllCompetitions(Long communityId, Long mainEventId) {
        List<Competition> raw;
        if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
        }

        List<Competition> filtered = new java.util.ArrayList<>();
        java.util.Map<Long, Boolean> eventCancelledCache = new java.util.HashMap<>();
        for (Competition c : raw) {
            if (c.getMainEventId() != null) {
                boolean isParentCancelled = eventCancelledCache.computeIfAbsent(c.getMainEventId(), id -> {
                    com.manacommunity.api.events.entity.CommunityEvent parent = eventRepository.findById(id).orElse(null);
                    return parent != null && parent.getStatus() == com.manacommunity.api.events.entity.CommunityEvent.EventStatus.CANCELLED;
                });
                if (isParentCancelled) {
                    continue;
                }
            }
            filtered.add(c);
        }
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public Competition getCompetitionById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", id));
    }

    @Override
    public Competition createCompetition(Long communityId, Competition competition) {
        validateDateWithinParentEvent(competition.getMainEventId(), competition.getDate());
        competition.setCommunityId(communityId);
        return repository.save(competition);
    }

    @Override
    public Competition updateCompetition(Long id, Long communityId, Competition updated) {
        Competition existing = getCompetitionById(id, communityId);
        validateDateWithinParentEvent(updated.getMainEventId(), updated.getDate());
        existing.setMainEventId(updated.getMainEventId());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setAgeGroup(updated.getAgeGroup());
        existing.setDate(updated.getDate());
        existing.setStartTime(updated.getStartTime());
        existing.setRegDeadline(updated.getRegDeadline());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        existing.setMaxParticipants(updated.getMaxParticipants());
        existing.setVenue(updated.getVenue());
        existing.setRules(updated.getRules());
        existing.setIsTeamEvent(updated.getIsTeamEvent());
        existing.setTeamSize(updated.getTeamSize());
        return repository.save(existing);
    }

    private void validateDateWithinParentEvent(Long mainEventId, LocalDate date) {
        if (mainEventId == null || date == null) return;
        eventRepository.findById(mainEventId).ifPresent(event -> {
            LocalDate eventStart = event.getStartDate();
            LocalDate eventEnd = event.getEndDate() != null ? event.getEndDate() : eventStart;
            if (date.isBefore(eventStart) || date.isAfter(eventEnd)) {
                throw new InvalidInputException("Competition date " + date + " is outside the event period (" + eventStart + " to " + eventEnd + ")");
            }
        });
    }

    @Override
    public void deleteCompetition(Long id, Long communityId) {
        Competition existing = getCompetitionById(id, communityId);
        String activityId = "comp-" + existing.getId();
        if (bookingRepo.existsByActivityIdAndStatusNot(activityId, "CANCELLED")) {
            throw new ManaCommunityException(
                    "Cannot delete competition with active bookings. Cancel the bookings first.",
                    HttpStatus.CONFLICT,
                    "COMPETITION_HAS_BOOKINGS"
            );
        }
        repository.delete(existing);
    }
}
