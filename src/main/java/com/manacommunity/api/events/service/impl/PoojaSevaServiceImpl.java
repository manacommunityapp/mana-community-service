package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDaySlot;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.PoojaSevaService;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PoojaSevaServiceImpl implements PoojaSevaService {

    private final PoojaSevaRepository repository;
    private final CommunityEventRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;

    public PoojaSevaServiceImpl(PoojaSevaRepository repository,
                                CommunityEventRepository eventRepository,
                                EventBookingRegistrationRepository bookingRepo) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId) {
        List<PoojaSeva> raw;
        if (communityId != null && mainEventId != null) {
            raw = repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(communityId, mainEventId);
        } else if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
        }

        // Filter out poojas whose parent community event is cancelled or whose own status is cancelled
        List<PoojaSeva> filtered = new java.util.ArrayList<>();
        java.util.Map<Long, Boolean> eventCancelledCache = new java.util.HashMap<>();
        for (PoojaSeva p : raw) {
            if (p.getMainEventId() != null) {
                boolean isParentCancelled = eventCancelledCache.computeIfAbsent(p.getMainEventId(), id -> {
                    CommunityEvent parent = eventRepository.findById(id).orElse(null);
                    return parent != null && parent.getStatus() == CommunityEvent.EventStatus.CANCELLED;
                });
                if (isParentCancelled) {
                    continue;
                }
            }
            filtered.add(p);
        }
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public PoojaSeva getPoojaSevaById(Long id, Long communityId) {
        if (communityId != null) {
            return repository.findByIdAndCommunityId(id, communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
    }

    @Override
    public PoojaSeva createPoojaSeva(Long communityId, PoojaSeva poojaSeva) {
        if (poojaSeva.getMainEventId() != null) {
            CommunityEvent parentEvent = eventRepository.findById(poojaSeva.getMainEventId())
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
        return repository.save(poojaSeva);
    }

    @Override
    public PoojaSeva updatePoojaSeva(Long id, Long communityId, PoojaSeva updated) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        Long resolvedMainEventId = updated.getMainEventId() != null
                ? updated.getMainEventId()
                : existing.getMainEventId();
        if (resolvedMainEventId != null) {
            CommunityEvent parentEvent = eventRepository.findById(resolvedMainEventId)
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
        if (updated.getItems() != null) {
            existing.getItems().clear();
            existing.getItems().addAll(updated.getItems());
        }
        if (updated.getStartTimes() != null) {
            existing.getStartTimes().clear();
            existing.getStartTimes().addAll(updated.getStartTimes());
        }
        if (updated.getDaySlots() != null) {
            existing.getDaySlots().clear();
            existing.getDaySlots().addAll(updated.getDaySlots());
        }
        if (updated.getTimeSlotConfig() != null) {
            existing.getTimeSlotConfig().clear();
            existing.getTimeSlotConfig().addAll(updated.getTimeSlotConfig());
        }
        existing.setNotes(updated.getNotes());
        return repository.save(existing);
    }

    @Override
    public void deletePoojaSeva(Long id, Long communityId) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        String activityId = "pooja-" + existing.getId();
        if (bookingRepo.existsByActivityIdAndStatusNot(activityId, "CANCELLED")) {
            throw new ManaCommunityException(
                    "Cannot delete pooja/seva with active bookings. Cancel the bookings first.",
                    HttpStatus.CONFLICT,
                    "POOJA_HAS_BOOKINGS"
            );
        }
        repository.delete(existing);
    }

    private void validateDateWithinParent(PoojaSeva poojaSeva, CommunityEvent parentEvent) {
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
            for (PoojaSevaDaySlot slot : poojaSeva.getDaySlots()) {
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
            for (PoojaSevaDayTimeSlot slot : poojaSeva.getTimeSlotConfig()) {
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
