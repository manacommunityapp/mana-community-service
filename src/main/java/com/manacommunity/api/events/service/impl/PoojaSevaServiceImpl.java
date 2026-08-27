package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.entity.EventPoojaSevaDaySlot;
import com.manacommunity.api.events.entity.EventPoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventPoojaSevaTimeSlotRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.repository.PoojaTypeRepository;
import com.manacommunity.api.events.service.PoojaSevaService;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PoojaSevaServiceImpl implements PoojaSevaService {

    private final PoojaSevaRepository repository;
    private final EventCommunityRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;
    private final PoojaTypeRepository poojaTypeRepository;
    private final EventPoojaSevaTimeSlotRepository timeSlotRepository;

    public PoojaSevaServiceImpl(PoojaSevaRepository repository,
                                EventCommunityRepository eventRepository,
                                EventBookingRegistrationRepository bookingRepo,
                                PoojaTypeRepository poojaTypeRepository,
                                EventPoojaSevaTimeSlotRepository timeSlotRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
        this.poojaTypeRepository = poojaTypeRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Attach persisted time-slots to the transient field so they appear in JSON responses. */
    private void populateTimeSlots(EventPoojaSeva p) {
        p.setTimeSlotConfig(
            timeSlotRepository.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(p.getId())
        );
    }

    private void populateTimeSlots(List<EventPoojaSeva> list) {
        list.forEach(this::populateTimeSlots);
    }

    /**
     * Persist the timeSlotConfig list for a given parent id.
     * Replaces all existing slots (delete-all → insert).
     */
    private void saveSlots(Long poojaSevaId, List<EventPoojaSevaDayTimeSlot> slots) {
        timeSlotRepository.deleteByPoojaSevaId(poojaSevaId);
        if (slots != null && !slots.isEmpty()) {
            slots.forEach(s -> s.setPoojaSevaId(poojaSevaId));
            timeSlotRepository.saveAll(slots);
        }
    }

    // ── read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EventPoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId) {
        List<EventPoojaSeva> raw;
        if (communityId != null && mainEventId != null) {
            raw = repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(communityId, mainEventId);
        } else if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
        }

        // Filter out poojas whose parent community event is cancelled
        List<EventPoojaSeva> filtered = new ArrayList<>();
        java.util.Map<Long, Boolean> eventCancelledCache = new java.util.HashMap<>();
        for (EventPoojaSeva p : raw) {
            if (p.getMainEventId() != null) {
                boolean isParentCancelled = eventCancelledCache.computeIfAbsent(p.getMainEventId(), id -> {
                    EventCommunity parent = eventRepository.findById(id).orElse(null);
                    return parent != null && parent.getStatus() == EventCommunity.EventStatus.CANCELLED;
                });
                if (isParentCancelled) continue;
            }
            filtered.add(p);
        }

        populateTimeSlots(filtered);
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public EventPoojaSeva getPoojaSevaById(Long id, Long communityId) {
        EventPoojaSeva p;
        if (communityId != null) {
            p = repository.findByIdAndCommunityId(id, communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
        } else {
            p = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
        }
        populateTimeSlots(p);
        return p;
    }

    // ── write ─────────────────────────────────────────────────────────────────

    @Override
    public EventPoojaSeva createPoojaSeva(Long communityId, EventPoojaSeva poojaSeva) {
        if (poojaSeva.getMainEventId() != null) {
            EventCommunity parentEvent = eventRepository.findById(poojaSeva.getMainEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Event", poojaSeva.getMainEventId()));
            if (communityId != null && parentEvent.getCommunity() != null
                    && !communityId.equals(parentEvent.getCommunity().getId())) {
                throw new ManaCommunityException(
                        "Parent event belongs to a different community",
                        HttpStatus.BAD_REQUEST,
                        "PARENT_EVENT_COMMUNITY_MISMATCH"
                );
            }
            validateDateWithinParent(poojaSeva, parentEvent);
        }
        poojaSeva.setCommunityId(communityId);
        if (poojaSeva.getPoojaTypeId() == null && poojaSeva.getType() != null && !poojaSeva.getType().trim().isEmpty()) {
            poojaTypeRepository.findFirstByNameIgnoreCase(poojaSeva.getType().trim())
                    .ifPresent(pt -> poojaSeva.setPoojaTypeId(pt.getId()));
        }
        if (poojaSeva.getNeedsRegistration() == null) {
            poojaSeva.setNeedsRegistration(true);
        }

        // Capture the requested slots before clearing the transient field
        List<EventPoojaSevaDayTimeSlot> requestedSlots = new ArrayList<>();
        if (Boolean.FALSE.equals(poojaSeva.getNeedsRegistration())) {
            // no slots needed
        } else if (Boolean.TRUE.equals(poojaSeva.getMultiDay())
                && poojaSeva.getDate() != null
                && poojaSeva.getEndDate() != null
                && poojaSeva.getStartTimes() != null
                && !poojaSeva.getStartTimes().isEmpty()
                && (poojaSeva.getTimeSlotConfig() == null || poojaSeva.getTimeSlotConfig().isEmpty())) {
            // Auto-generate per-day slots from startTimes + duration
            DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
            LocalDate cur = poojaSeva.getDate();
            while (!cur.isAfter(poojaSeva.getEndDate())) {
                for (String time : poojaSeva.getStartTimes()) {
                    String endTime = null;
                    if (poojaSeva.getDuration() != null && poojaSeva.getDuration() > 0) {
                        endTime = LocalTime.parse(time, hhmm).plusMinutes(poojaSeva.getDuration()).format(hhmm);
                    }
                    requestedSlots.add(new EventPoojaSevaDayTimeSlot(
                            null, cur, time, endTime, poojaSeva.getName(),
                            poojaSeva.getSlots() != null ? poojaSeva.getSlots() : 0
                    ));
                }
                cur = cur.plusDays(1);
            }
        } else if (poojaSeva.getTimeSlotConfig() != null) {
            requestedSlots.addAll(poojaSeva.getTimeSlotConfig());
        }

        // Clear transient field before persisting parent
        poojaSeva.setTimeSlotConfig(new ArrayList<>());
        EventPoojaSeva saved = repository.save(poojaSeva);

        // Now persist slots with the real parent id
        saveSlots(saved.getId(), requestedSlots);
        saved.setTimeSlotConfig(
            timeSlotRepository.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(saved.getId())
        );
        return saved;
    }

    @Override
    public EventPoojaSeva updatePoojaSeva(Long id, Long communityId, EventPoojaSeva updated) {
        EventPoojaSeva existing = getPoojaSevaById(id, communityId);
        Long resolvedMainEventId = updated.getMainEventId() != null
                ? updated.getMainEventId()
                : existing.getMainEventId();
        if (resolvedMainEventId != null) {
            EventCommunity parentEvent = eventRepository.findById(resolvedMainEventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Event", resolvedMainEventId));
            if (communityId != null && parentEvent.getCommunity() != null
                    && !communityId.equals(parentEvent.getCommunity().getId())) {
                throw new ManaCommunityException(
                        "Parent event belongs to a different community",
                        HttpStatus.BAD_REQUEST,
                        "PARENT_EVENT_COMMUNITY_MISMATCH"
                );
            }
            validateDateWithinParent(updated, parentEvent);
        }
        existing.setMainEventId(updated.getMainEventId());
        Long resolvedPoojaTypeId = updated.getPoojaTypeId();
        if (resolvedPoojaTypeId == null && updated.getType() != null && !updated.getType().trim().isEmpty()) {
            resolvedPoojaTypeId = poojaTypeRepository.findFirstByNameIgnoreCase(updated.getType().trim())
                    .map(com.manacommunity.api.events.entity.EventPoojaType::getId)
                    .orElse(existing.getPoojaTypeId());
        }
        existing.setPoojaTypeId(resolvedPoojaTypeId);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setDate(updated.getDate());
        existing.setEndDate(updated.getEndDate());
        existing.setMultiDay(updated.getMultiDay());
        existing.setStartTime(updated.getStartTime());
        existing.setDuration(updated.getDuration());
        existing.setMandap(updated.getMandap());
        existing.setPandit(updated.getPandit());
        if (updated.getSlots() != null) {
            String activityId = "pooja-" + existing.getId();
            long activeBookings = bookingRepo.countByActivityIdAndStatusNot(activityId, "CANCELLED");
            if (updated.getSlots() < activeBookings) {
                throw new ManaCommunityException(
                        "Slots cannot be less than active bookings (" + activeBookings + ")",
                        HttpStatus.CONFLICT,
                        "SLOTS_BELOW_BOOKINGS"
                );
            }
        }
        existing.setSlots(updated.getSlots());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        boolean resolvedNeedsRegistration = updated.getNeedsRegistration() != null ? updated.getNeedsRegistration() : true;
        existing.setNeedsRegistration(resolvedNeedsRegistration);
        if (updated.getItems() != null) {
            existing.getItems().clear();
            existing.getItems().addAll(updated.getItems());
        }
        if (updated.getStartTimes() != null && !updated.getStartTimes().isEmpty()) {
            existing.getStartTimes().clear();
            existing.getStartTimes().addAll(updated.getStartTimes());
        }
        if (updated.getDaySlots() != null) {
            existing.getDaySlots().clear();
            existing.getDaySlots().addAll(updated.getDaySlots());
        }
        existing.setNotes(updated.getNotes());

        // Save parent first
        existing.setTimeSlotConfig(new ArrayList<>());
        EventPoojaSeva savedExisting = repository.save(existing);

        // Update slots: delete-all + re-insert
        if (!resolvedNeedsRegistration) {
            timeSlotRepository.deleteByPoojaSevaId(savedExisting.getId());
        } else if (updated.getTimeSlotConfig() != null) {
            saveSlots(savedExisting.getId(), updated.getTimeSlotConfig());
        }

        savedExisting.setTimeSlotConfig(
            timeSlotRepository.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(savedExisting.getId())
        );
        return savedExisting;
    }

    @Override
    public void deletePoojaSeva(Long id, Long communityId) {
        EventPoojaSeva existing = getPoojaSevaById(id, communityId);
        String activityId = "pooja-" + existing.getId();
        if (bookingRepo.existsByActivityIdAndStatusNot(activityId, "CANCELLED")) {
            throw new ManaCommunityException(
                    "Cannot delete pooja/seva with active bookings. Cancel the bookings first.",
                    HttpStatus.CONFLICT,
                    "POOJA_HAS_BOOKINGS"
            );
        }
        // DB ON DELETE CASCADE handles the child rows in event_pooja_seva_time_slots
        repository.delete(existing);
    }

    // ── validation ────────────────────────────────────────────────────────────

    private void validateDateWithinParent(EventPoojaSeva poojaSeva, EventCommunity parentEvent) {
        LocalDate parentStart = parentEvent.getStartDate();
        LocalDate parentEnd = parentEvent.getEndDate() != null ? parentEvent.getEndDate() : parentStart;

        LocalDate poojaStart = poojaSeva.getDate();
        if (poojaStart != null && (poojaStart.isBefore(parentStart) || poojaStart.isAfter(parentEnd))) {
            throw new ManaCommunityException(
                    "Pooja/Seva date must be within the parent event range (" + parentStart + " to " + parentEnd + ")",
                    HttpStatus.BAD_REQUEST,
                    "POOJA_DATE_OUT_OF_RANGE"
            );
        }

        LocalDate poojaEnd = poojaSeva.getEndDate();
        if (poojaEnd != null && (poojaEnd.isBefore(parentStart) || poojaEnd.isAfter(parentEnd))) {
            throw new ManaCommunityException(
                    "Pooja/Seva end date must be within the parent event range (" + parentStart + " to " + parentEnd + ")",
                    HttpStatus.BAD_REQUEST,
                    "POOJA_DATE_OUT_OF_RANGE"
            );
        }

        if (poojaSeva.getDaySlots() != null) {
            for (EventPoojaSevaDaySlot slot : poojaSeva.getDaySlots()) {
                LocalDate slotDate = slot.getSlotDate();
                if (slotDate != null && (slotDate.isBefore(parentStart) || slotDate.isAfter(parentEnd))) {
                    throw new ManaCommunityException(
                            "Pooja/Seva slot date " + slotDate + " is outside the parent event range (" + parentStart + " to " + parentEnd + ")",
                            HttpStatus.BAD_REQUEST,
                            "POOJA_DATE_OUT_OF_RANGE"
                    );
                }
            }
        }

        if (poojaSeva.getTimeSlotConfig() != null) {
            for (EventPoojaSevaDayTimeSlot slot : poojaSeva.getTimeSlotConfig()) {
                LocalDate slotDate = slot.getSlotDate();
                if (slotDate != null && (slotDate.isBefore(parentStart) || slotDate.isAfter(parentEnd))) {
                    throw new ManaCommunityException(
                            "Pooja/Seva time slot date " + slotDate + " is outside the parent event range (" + parentStart + " to " + parentEnd + ")",
                            HttpStatus.BAD_REQUEST,
                            "POOJA_DATE_OUT_OF_RANGE"
                    );
                }
            }
        }
    }
}
