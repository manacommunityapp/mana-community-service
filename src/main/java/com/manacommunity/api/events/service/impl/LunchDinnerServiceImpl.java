package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.dto.LunchDinnerRegistrationSummaryResponse;
import com.manacommunity.api.events.dto.LunchDinnerSummaryResponse;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.EventLunchDinner;
import com.manacommunity.api.events.entity.EventMealRegistration;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventMealRegistrationRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.service.LunchDinnerService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class LunchDinnerServiceImpl implements LunchDinnerService {

    private final LunchDinnerRepository repository;
    private final EventCommunityRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;
    private final EventMealRegistrationRepository mealRepo;

    public LunchDinnerServiceImpl(LunchDinnerRepository repository,
                                  EventCommunityRepository eventRepository,
                                  EventBookingRegistrationRepository bookingRepo,
                                  EventMealRegistrationRepository mealRepo) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
        this.mealRepo = mealRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventLunchDinner> getAllLunchDinners(Long communityId, Long mainEventId) {
        List<EventLunchDinner> raw;
        if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
        }

        List<EventLunchDinner> filtered = new java.util.ArrayList<>();
        java.util.Map<Long, Boolean> eventCancelledCache = new java.util.HashMap<>();
        for (EventLunchDinner m : raw) {
            if (m.getMainEventId() != null) {
                boolean isParentCancelled = eventCancelledCache.computeIfAbsent(m.getMainEventId(), id -> {
                    com.manacommunity.api.events.entity.EventCommunity parent = eventRepository.findById(id).orElse(null);
                    return parent != null && parent.getStatus() == com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED;
                });
                if (isParentCancelled) {
                    continue;
                }
            }
            filtered.add(m);
        }
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchDinnerSummaryResponse> getAllLunchDinnerSummaries(Long communityId, Long mainEventId) {
        List<EventLunchDinner> meals = getAllLunchDinners(communityId, mainEventId);
        List<EventBookingRegistration> allBookingRegs = communityId != null
                ? bookingRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                : bookingRepo.findAll();

        List<EventMealRegistration> allMealRegs = communityId != null
                ? mealRepo.findByCommunityId(communityId)
                : mealRepo.findAll();

        List<LunchDinnerSummaryResponse> result = new ArrayList<>();
        for (EventLunchDinner m : meals) {
            String actId1 = "meal-" + m.getId();
            String actId2 = "food-" + m.getId();
            String actId3 = String.valueOf(m.getId());

            List<EventBookingRegistration> bookingRegs = allBookingRegs.stream()
                    .filter(r -> {
                        if (r.getStatus() != null && (r.getStatus().equalsIgnoreCase("CANCELLED") || r.getStatus().equalsIgnoreCase("REJECTED"))) {
                            return false;
                        }
                        String act = r.getActivityId() != null ? r.getActivityId() : "";
                        String title = r.getActivityTitle() != null ? r.getActivityTitle() : "";
                        return act.equals(actId1) || act.equals(actId2) || act.equals(actId3) || title.equalsIgnoreCase(m.getName());
                    })
                    .toList();

            List<EventMealRegistration> mealRegs = allMealRegs.stream()
                    .filter(r -> {
                        if (m.getMainEventId() != null && r.getEvent() != null && !r.getEvent().getId().equals(m.getMainEventId())) {
                            return false;
                        }
                        if (r.getMealDate() != null && m.getDate() != null && !r.getMealDate().equals(m.getDate())) {
                            return false;
                        }
                        if (r.getMealType() != null && m.getMealType() != null) {
                            String rType = r.getMealType().name().toLowerCase();
                            String mType = m.getMealType().toLowerCase();
                            if (!mType.contains(rType) && !rType.contains(mType)) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .toList();

            long bookedCount = bookingRegs.size() + mealRegs.size();
            long attendeeHeadcount = bookingRegs.stream()
                    .mapToLong(r -> r.getDevoteeCount() != null ? r.getDevoteeCount() : 1)
                    .sum() + mealRegs.stream()
                    .mapToLong(r -> r.getHeadCount() != null ? r.getHeadCount() : 1)
                    .sum();

            result.add(LunchDinnerSummaryResponse.builder()
                    .id(m.getId())
                    .communityId(m.getCommunityId())
                    .mainEventId(m.getMainEventId())
                    .name(m.getName())
                    .mealType(m.getMealType())
                    .date(m.getDate())
                    .startTime(m.getStartTime())
                    .endTime(m.getEndTime())
                    .venue(m.getVenue())
                    .targetPlates(m.getTargetPlates())
                    .caterer(m.getCaterer())
                    .dietType(m.getDietType())
                    .fee(m.getFee())
                    .isFree(m.getIsFree())
                    .needsRegistration(m.getNeedsRegistration())
                    .menuItems(m.getMenuItems())
                    .notes(m.getNotes())
                    .bookedCount(bookedCount)
                    .attendeeHeadcount(attendeeHeadcount)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchDinnerRegistrationSummaryResponse> getRegistrationsForMeal(Long mealId, Long communityId) {
        EventLunchDinner meal = getLunchDinnerById(mealId, communityId);
        String actId1 = "meal-" + meal.getId();
        String actId2 = "food-" + meal.getId();
        String actId3 = String.valueOf(meal.getId());

        List<LunchDinnerRegistrationSummaryResponse> combined = new ArrayList<>();

        // 1. Fetch from event_booking_registrations
        List<EventBookingRegistration> allBookingRegs = communityId != null
                ? bookingRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                : bookingRepo.findAll();

        List<EventBookingRegistration> bookingRegs = allBookingRegs.stream()
                .filter(r -> {
                    String act = r.getActivityId() != null ? r.getActivityId() : "";
                    String title = r.getActivityTitle() != null ? r.getActivityTitle() : "";
                    return act.equals(actId1) || act.equals(actId2) || act.equals(actId3) || title.equalsIgnoreCase(meal.getName());
                })
                .toList();

        for (EventBookingRegistration r : bookingRegs) {
            String userPhone = r.getUser() != null ? r.getUser().getPhone() : null;
            String userEmail = r.getUser() != null ? r.getUser().getEmail() : null;
            combined.add(LunchDinnerRegistrationSummaryResponse.builder()
                .id(r.getId())
                .regCode(r.getRegCode())
                .activityId(r.getActivityId())
                .activityTitle(r.getActivityTitle())
                .category(r.getCategory())
                .participantName(r.getParticipantName())
                .email(userEmail)
                .phone(userPhone)
                .gotram(r.getGotram())
                .attendingDevotees(r.getAttendingDevotees())
                .devoteeCount(r.getDevoteeCount() != null ? r.getDevoteeCount() : 1)
                .eventDate(r.getEventDate())
                .eventTime(r.getEventTime())
                .venue(r.getVenue())
                .bookingFee(r.getBookingFee())
                .paymentStatus(r.getPaymentStatus())
                .status(r.getStatus())
                .notes(r.getOverrideReason())
                .createdAt(r.getCreatedAt())
                .build());
        }

        // 2. Fetch from event_meal_registrations
        List<EventMealRegistration> allMealRegs = communityId != null
                ? mealRepo.findByCommunityId(communityId)
                : mealRepo.findAll();

        List<EventMealRegistration> mealRegs = allMealRegs.stream()
                .filter(r -> {
                    if (meal.getMainEventId() != null && r.getEvent() != null && !r.getEvent().getId().equals(meal.getMainEventId())) {
                        return false;
                    }
                    if (r.getMealDate() != null && meal.getDate() != null && !r.getMealDate().equals(meal.getDate())) {
                        return false;
                    }
                    if (r.getMealType() != null && meal.getMealType() != null) {
                        String rType = r.getMealType().name().toLowerCase();
                        String mType = meal.getMealType().toLowerCase();
                        if (!mType.contains(rType) && !rType.contains(mType)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();

        for (EventMealRegistration mr : mealRegs) {
            String name = mr.getUser() != null
                    ? (mr.getUser().getFullName() != null ? mr.getUser().getFullName() : mr.getUser().getUsername())
                    : "Devotee";
            String phone = mr.getUser() != null ? mr.getUser().getPhone() : null;
            String email = mr.getUser() != null ? mr.getUser().getEmail() : null;
            String dietary = mr.getDietaryPref() != null ? mr.getDietaryPref().name() : "";
            String notes = dietary + (mr.getAllergies() != null && !mr.getAllergies().isBlank() ? " (" + mr.getAllergies() + ")" : "");

            combined.add(LunchDinnerRegistrationSummaryResponse.builder()
                .id(mr.getId())
                .regCode("MEAL-" + mr.getId())
                .activityId("meal-" + meal.getId())
                .activityTitle(meal.getName())
                .category("Meal")
                .participantName(name)
                .email(email)
                .phone(phone)
                .devoteeCount(mr.getHeadCount() != null ? mr.getHeadCount() : 1)
                .eventDate(mr.getMealDate() != null ? mr.getMealDate().toString() : "")
                .eventTime(meal.getStartTime() != null ? meal.getStartTime().toString() : "")
                .venue(meal.getVenue())
                .bookingFee(meal.getFee() != null ? meal.getFee().doubleValue() : 0.0)
                .paymentStatus(Boolean.TRUE.equals(meal.getIsFree()) ? "FREE" : "PAID")
                .status("CONFIRMED")
                .notes(notes)
                .createdAt(mr.getCreatedAt())
                .build());
        }

        return combined;
    }

    @Override
    @Transactional(readOnly = true)
    public EventLunchDinner getLunchDinnerById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch/Dinner event", id));
    }

    @Override
    public EventLunchDinner createLunchDinner(Long communityId, EventLunchDinner lunchDinner) {
        validateDateWithinParentEvent(lunchDinner.getMainEventId(), lunchDinner.getDate());
        lunchDinner.setCommunityId(communityId);
        return repository.save(lunchDinner);
    }

    @Override
    public EventLunchDinner updateLunchDinner(Long id, Long communityId, EventLunchDinner updated) {
        EventLunchDinner existing = getLunchDinnerById(id, communityId);
        validateDateWithinParentEvent(updated.getMainEventId(), updated.getDate());
        existing.setMainEventId(updated.getMainEventId());
        existing.setName(updated.getName());
        existing.setMealType(updated.getMealType());
        existing.setDate(updated.getDate());
        existing.setStartTime(updated.getStartTime());
        existing.setEndTime(updated.getEndTime());
        existing.setVenue(updated.getVenue());
        existing.setTargetPlates(updated.getTargetPlates());
        existing.setCaterer(updated.getCaterer());
        existing.setDietType(updated.getDietType());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        if (updated.getMenuItems() != null) {
            existing.setMenuItems(updated.getMenuItems());
        }
        existing.setNotes(updated.getNotes());
        if (updated.getNeedsRegistration() != null) {
            existing.setNeedsRegistration(updated.getNeedsRegistration());
        }
        return repository.save(existing);
    }


    private void validateDateWithinParentEvent(Long mainEventId, LocalDate date) {
        if (mainEventId == null || date == null) return;
        eventRepository.findById(mainEventId).ifPresent(event -> {
            LocalDate eventStart = event.getStartDate();
            LocalDate eventEnd = event.getEndDate() != null ? event.getEndDate() : eventStart;
            if (date.isBefore(eventStart) || date.isAfter(eventEnd)) {
                throw new InvalidInputException("Lunch/Dinner date " + date + " is outside the event period (" + eventStart + " to " + eventEnd + ")");
            }
        });
    }

    @Override
    public void deleteLunchDinner(Long id, Long communityId) {
        EventLunchDinner existing = getLunchDinnerById(id, communityId);
        if (bookingRepo.existsByActivityIdAndStatusNot("food-" + existing.getId(), "CANCELLED")
                || bookingRepo.existsByActivityIdAndStatusNot("meal-" + existing.getId(), "CANCELLED")) {
            throw new ManaCommunityException(
                    "Cannot delete lunch/dinner event with active bookings. Cancel the bookings first.",
                    HttpStatus.CONFLICT,
                    "FOOD_HAS_BOOKINGS"
            );
        }
        repository.delete(existing);
    }
}
