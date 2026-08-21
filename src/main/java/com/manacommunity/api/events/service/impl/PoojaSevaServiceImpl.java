package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.PoojaSevaService;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PoojaSevaServiceImpl implements PoojaSevaService {

    private final PoojaSevaRepository repository;
    private final CommunityEventRepository eventRepository;

    public PoojaSevaServiceImpl(PoojaSevaRepository repository, CommunityEventRepository eventRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId) {
        requireCommunityId(communityId);
        if (mainEventId != null) {
            validateMainEventBelongsToCommunity(mainEventId, communityId);
            return repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(communityId, mainEventId);
        }
        return repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
    }

    @Override
    @Transactional(readOnly = true)
    public PoojaSeva getPoojaSevaById(Long id, Long communityId) {
        requireCommunityId(communityId);
        return repository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
    }

    @Override
    public PoojaSeva createPoojaSeva(Long communityId, PoojaSeva poojaSeva) {
        requireCommunityId(communityId);
        validateMainEventBelongsToCommunity(poojaSeva.getMainEventId(), communityId);
        validateDatesWithinParentEvent(poojaSeva.getMainEventId(), poojaSeva.getDate(), poojaSeva.getEndDate());
        poojaSeva.setCommunityId(communityId);
        normalizeSlotAvailability(poojaSeva);
        return repository.save(poojaSeva);
    }

    @Override
    public PoojaSeva updatePoojaSeva(Long id, Long communityId, PoojaSeva updated) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        validateMainEventBelongsToCommunity(updated.getMainEventId(), communityId);
        validateDatesWithinParentEvent(updated.getMainEventId(), updated.getDate(), updated.getEndDate());
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
        existing.setSlots(updated.getSlots());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        if (updated.getItems() != null) {
            existing.setItems(updated.getItems());
        }
        if (updated.getStartTimes() != null) {
            existing.setStartTimes(updated.getStartTimes());
        }
        if (updated.getDaySlots() != null) {
            existing.setDaySlots(updated.getDaySlots());
        }
        if (updated.getTimeSlotConfig() != null) {
            existing.setTimeSlotConfig(updated.getTimeSlotConfig());
        }
        existing.setNotes(updated.getNotes());
        normalizeSlotAvailability(existing);
        return repository.save(existing);
    }

    @Override
    public void deletePoojaSeva(Long id, Long communityId) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        repository.delete(existing);
    }

    private void validateDatesWithinParentEvent(Long mainEventId, LocalDate date, LocalDate endDate) {
        if (mainEventId == null || date == null) return;
        eventRepository.findById(mainEventId).ifPresent(event -> {
            LocalDate eventStart = event.getStartDate();
            LocalDate eventEnd = event.getEndDate() != null ? event.getEndDate() : eventStart;
            if (date.isBefore(eventStart) || date.isAfter(eventEnd)) {
                throw new InvalidInputException("Pooja/Seva date " + date + " is outside the event period (" + eventStart + " to " + eventEnd + ")");
            }
            if (endDate != null && endDate.isAfter(eventEnd)) {
                throw new InvalidInputException("Pooja/Seva end date " + endDate + " is after the event end date (" + eventEnd + ")");
            }
        });
    }

    private void validateMainEventBelongsToCommunity(Long mainEventId, Long communityId) {
        if (mainEventId == null) return;
        eventRepository.findByIdAndCommunity_Id(mainEventId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Main Event", mainEventId));
    }

    private void requireCommunityId(Long communityId) {
        if (communityId == null) {
            throw new IllegalArgumentException("Community ID is required");
        }
    }

    private void normalizeSlotAvailability(PoojaSeva poojaSeva) {
        if (poojaSeva == null) return;

        boolean multiDay = Boolean.TRUE.equals(poojaSeva.getMultiDay());
        if (!multiDay) {
            if (poojaSeva.getSlots() == null) {
                Integer derivedSlots = firstConfiguredSlotCount(poojaSeva);
                if (derivedSlots != null) {
                    poojaSeva.setSlots(derivedSlots);
                }
            }
            return;
        }

        if (poojaSeva.getDate() == null) return;

        LocalDate startDate = poojaSeva.getDate();
        LocalDate endDate = poojaSeva.getEndDate();
        if (endDate == null || endDate.isBefore(startDate)) {
            endDate = startDate;
            poojaSeva.setEndDate(endDate);
        }

        List<String> startTimes = normalizedStartTimes(poojaSeva);
        if (startTimes.isEmpty()) return;

        Map<String, PoojaSevaDayTimeSlot> existingByKey = new LinkedHashMap<>();
        if (poojaSeva.getTimeSlotConfig() != null) {
            for (PoojaSevaDayTimeSlot slot : poojaSeva.getTimeSlotConfig()) {
                if (slot == null || slot.getSlotDate() == null || isBlank(slot.getStartTime())) continue;
                existingByKey.put(slotKey(slot.getSlotDate(), slot.getStartTime()), slot);
            }
        }

        List<PoojaSevaDayTimeSlot> normalized = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (String startTime : startTimes) {
                PoojaSevaDayTimeSlot slot = existingByKey.get(slotKey(date, startTime));
                if (slot == null) {
                    slot = new PoojaSevaDayTimeSlot(date, startTime, poojaSeva.getSlots());
                } else {
                    slot.setSlotDate(date);
                    slot.setStartTime(startTime);
                    if (slot.getSlotCount() == null) {
                        slot.setSlotCount(poojaSeva.getSlots());
                    }
                }
                normalized.add(slot);
            }
        }
        poojaSeva.setTimeSlotConfig(normalized);
    }

    private Integer firstConfiguredSlotCount(PoojaSeva poojaSeva) {
        if (poojaSeva.getTimeSlotConfig() != null) {
            for (PoojaSevaDayTimeSlot slot : poojaSeva.getTimeSlotConfig()) {
                if (slot != null && slot.getSlotCount() != null) {
                    return slot.getSlotCount();
                }
            }
        }
        if (poojaSeva.getDaySlots() != null) {
            for (var slot : poojaSeva.getDaySlots()) {
                if (slot != null && slot.getSlotCount() != null) {
                    return slot.getSlotCount();
                }
            }
        }
        return null;
    }

    private List<String> normalizedStartTimes(PoojaSeva poojaSeva) {
        Map<String, String> ordered = new LinkedHashMap<>();
        if (poojaSeva.getStartTimes() != null) {
            for (String startTime : poojaSeva.getStartTimes()) {
                if (!isBlank(startTime)) {
                    String clean = startTime.trim();
                    ordered.put(clean, clean);
                }
            }
        }
        if (ordered.isEmpty() && poojaSeva.getStartTime() != null) {
            String clean = poojaSeva.getStartTime().toString();
            ordered.put(clean, clean);
        }
        return new ArrayList<>(ordered.values());
    }

    private String slotKey(LocalDate date, String startTime) {
        return date + "|" + startTime.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
