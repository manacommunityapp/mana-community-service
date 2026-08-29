package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.MealRegistrationRequest;
import com.manacommunity.api.events.dto.MealRegistrationResponse;
import com.manacommunity.api.events.dto.MealSummaryResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventLunchDinner;
import com.manacommunity.api.events.entity.EventMealRegistration;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventMealRegistrationRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventMealService {

    private final EventMealRegistrationRepository mealRepo;
    private final EventCommunityRepository eventRepo;
    private final LunchDinnerRepository lunchDinnerRepo;

    @Transactional(readOnly = true)
    public MealRegistrationResponse getUserMeals(Long eventId, Long userId) {
        List<EventMealRegistration> list = mealRepo.findByEventIdAndUserId(eventId, userId);

        MealRegistrationResponse response = new MealRegistrationResponse();
        response.setEventId(eventId);
        response.setUserId(userId);

        if (list.isEmpty()) {
            response.setMeals(Collections.emptyList());
            return response;
        }

        EventMealRegistration first = list.get(0);
        response.setDietaryPref(first.getDietaryPref() != null ? first.getDietaryPref().name() : "VEG");
        response.setAllergies(first.getAllergies());

        // Group by mealDate
        Map<LocalDate, List<EventMealRegistration>> grouped = list.stream()
                .collect(Collectors.groupingBy(EventMealRegistration::getMealDate, TreeMap::new, Collectors.toList()));

        List<MealRegistrationResponse.DayMealResponse> dayMeals = new ArrayList<>();
        for (Map.Entry<LocalDate, List<EventMealRegistration>> entry : grouped.entrySet()) {
            MealRegistrationResponse.DayMealResponse dmr = new MealRegistrationResponse.DayMealResponse();
            dmr.setDate(entry.getKey().toString());

            boolean morning = entry.getValue().stream().anyMatch(m -> m.getMealType() == EventMealRegistration.MealType.MORNING);
            boolean lunch   = entry.getValue().stream().anyMatch(m -> m.getMealType() == EventMealRegistration.MealType.LUNCH);
            boolean dinner  = entry.getValue().stream().anyMatch(m -> m.getMealType() == EventMealRegistration.MealType.DINNER);
            int headCount = entry.getValue().stream().mapToInt(m -> m.getHeadCount() != null ? m.getHeadCount() : 1).max().orElse(1);

            dmr.setMorning(morning);
            dmr.setLunch(lunch);
            dmr.setDinner(dinner);
            dmr.setHeadCount(headCount);
            dayMeals.add(dmr);
        }

        response.setMeals(dayMeals);
        return response;
    }

    @Transactional
    public MealRegistrationResponse saveMeals(Long eventId, MealRegistrationRequest req, AppUser user) {
        EventCommunity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        mealRepo.deleteByEventIdAndUserId(eventId, user.getId());

        EventMealRegistration.DietaryPref dietaryPref = EventMealRegistration.DietaryPref.VEG;
        if (req.getDietaryPref() != null && !req.getDietaryPref().isBlank()) {
            try {
                dietaryPref = EventMealRegistration.DietaryPref.valueOf(req.getDietaryPref().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        if (req.getMeals() != null) {
            Long communityId = event.getCommunity() != null ? event.getCommunity().getId() : null;

            for (MealRegistrationRequest.DayMeal dm : req.getMeals()) {
                if (dm.getDate() == null) continue;
                int headCount = dm.getHeadCount() > 0 ? dm.getHeadCount() : 1;

                if (dm.isMorning()) {
                    checkMealCapacity(eventId, dm.getDate(), EventMealRegistration.MealType.MORNING, headCount, user.getId());
                    EventLunchDinner ldMorning = lunchDinnerRepo.findByEventAndDateAndType(eventId, dm.getDate(), "MORNING").orElse(null);
                    mealRepo.save(EventMealRegistration.builder()
                            .event(event).lunchDinner(ldMorning).user(user).communityId(communityId)
                            .mealDate(dm.getDate()).mealType(EventMealRegistration.MealType.MORNING)
                            .headCount(headCount).dietaryPref(dietaryPref).allergies(req.getAllergies())
                            .build());
                }

                if (dm.isLunch()) {
                    checkMealCapacity(eventId, dm.getDate(), EventMealRegistration.MealType.LUNCH, headCount, user.getId());
                    EventLunchDinner ldLunch = lunchDinnerRepo.findByEventAndDateAndType(eventId, dm.getDate(), "LUNCH").orElse(null);
                    mealRepo.save(EventMealRegistration.builder()
                            .event(event).lunchDinner(ldLunch).user(user).communityId(communityId)
                            .mealDate(dm.getDate()).mealType(EventMealRegistration.MealType.LUNCH)
                            .headCount(headCount).dietaryPref(dietaryPref).allergies(req.getAllergies())
                            .build());
                }

                if (dm.isDinner()) {
                    checkMealCapacity(eventId, dm.getDate(), EventMealRegistration.MealType.DINNER, headCount, user.getId());
                    EventLunchDinner ldDinner = lunchDinnerRepo.findByEventAndDateAndType(eventId, dm.getDate(), "DINNER").orElse(null);
                    mealRepo.save(EventMealRegistration.builder()
                            .event(event).lunchDinner(ldDinner).user(user).communityId(communityId)
                            .mealDate(dm.getDate()).mealType(EventMealRegistration.MealType.DINNER)
                            .headCount(headCount).dietaryPref(dietaryPref).allergies(req.getAllergies())
                            .build());
                }
            }
        }

        return getUserMeals(eventId, user.getId());
    }

    @Transactional(readOnly = true)
    public MealSummaryResponse getMealSummary(Long eventId) {
        List<EventMealRegistration> list = mealRepo.findByEventIdOrdered(eventId);

        MealSummaryResponse response = new MealSummaryResponse();
        response.setEventId(eventId);

        // Group by mealDate
        Map<LocalDate, List<EventMealRegistration>> grouped = list.stream()
                .collect(Collectors.groupingBy(EventMealRegistration::getMealDate, TreeMap::new, Collectors.toList()));

        List<MealSummaryResponse.DaySummary> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<EventMealRegistration>> entry : grouped.entrySet()) {
            MealSummaryResponse.DaySummary ds = new MealSummaryResponse.DaySummary();
            ds.setDate(entry.getKey().toString());

            MealSummaryResponse.MealBreakdown morning = new MealSummaryResponse.MealBreakdown();
            MealSummaryResponse.MealBreakdown lunch   = new MealSummaryResponse.MealBreakdown();
            MealSummaryResponse.MealBreakdown dinner  = new MealSummaryResponse.MealBreakdown();

            for (EventMealRegistration m : entry.getValue()) {
                int hc = m.getHeadCount() != null ? m.getHeadCount() : 1;
                EventMealRegistration.DietaryPref pref = m.getDietaryPref() != null ? m.getDietaryPref() : EventMealRegistration.DietaryPref.VEG;
                MealSummaryResponse.MealBreakdown target = switch (m.getMealType()) {
                    case MORNING -> morning;
                    case LUNCH   -> lunch;
                    case DINNER  -> dinner;
                };
                target.setTotalHeads(target.getTotalHeads() + hc);
                switch (pref) {
                    case VEG    -> target.setVeg(target.getVeg() + hc);
                    case VEGAN  -> target.setVegan(target.getVegan() + hc);
                    case JAIN   -> target.setJain(target.getJain() + hc);
                    case NONVEG -> target.setNonveg(target.getNonveg() + hc);
                }
            }

            ds.setMorning(morning);
            ds.setLunch(lunch);
            ds.setDinner(dinner);
            days.add(ds);
        }

        response.setDays(days);
        return response;
    }

    /**
     * Register (or update) a single meal slot for a user based on an EventLunchDinner record.
     * Acts as upsert: if the user already has a row for this event+date+mealType, updates headCount only.
     */
    @Transactional
    public MealRegistrationResponse registerSingleMeal(Long lunchDinnerId, int headCount,
                                                        String dietaryPref, AppUser user) {
        EventLunchDinner ld = lunchDinnerRepo.findById(lunchDinnerId)
                .orElseThrow(() -> new ResourceNotFoundException("LunchDinner", lunchDinnerId));

        if (ld.getMainEventId() == null) {
            throw new ResourceNotFoundException("Meal has no associated event", lunchDinnerId);
        }

        EventCommunity event = eventRepo.findById(ld.getMainEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", ld.getMainEventId()));

        LocalDate mealDate = ld.getDate();
        EventMealRegistration.MealType mealType;
        try {
            mealType = EventMealRegistration.MealType.valueOf(
                    ld.getMealType() != null ? ld.getMealType().toUpperCase() : "LUNCH");
        } catch (IllegalArgumentException e) {
            mealType = EventMealRegistration.MealType.LUNCH;
        }

        EventMealRegistration.DietaryPref dp = EventMealRegistration.DietaryPref.VEG;
        if (dietaryPref != null && !dietaryPref.isBlank()) {
            try { dp = EventMealRegistration.DietaryPref.valueOf(dietaryPref.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        int requestedCount = headCount > 0 ? headCount : 1;

        // Upsert: update existing row or create new
        EventMealRegistration existing = mealRepo
                .findByEventIdAndUserIdAndMealDateAndMealType(event.getId(), user.getId(), mealDate, mealType)
                .orElse(null);

        if (existing != null) {
            existing.setHeadCount(requestedCount);
            existing.setDietaryPref(dp);
            existing.setLunchDinner(ld);
            mealRepo.save(existing);
        } else {
            checkMealCapacity(event.getId(), mealDate, mealType, requestedCount, user.getId());
            mealRepo.save(EventMealRegistration.builder()
                    .event(event).lunchDinner(ld).user(user).communityId(ld.getCommunityId())
                    .mealDate(mealDate).mealType(mealType)
                    .headCount(requestedCount).dietaryPref(dp)
                    .build());
        }

        return getUserMeals(event.getId(), user.getId());
    }

    /**
     * Update only the headCount of an existing meal registration for a specific slot.
     */
    @Transactional
    public MealRegistrationResponse updateMealHeadCount(Long lunchDinnerId, int headCount, AppUser user) {
        EventLunchDinner ld = lunchDinnerRepo.findById(lunchDinnerId)
                .orElseThrow(() -> new ResourceNotFoundException("LunchDinner", lunchDinnerId));

        if (ld.getMainEventId() == null) throw new ResourceNotFoundException("Meal has no associated event", lunchDinnerId);

        EventMealRegistration.MealType mealType;
        try {
            mealType = EventMealRegistration.MealType.valueOf(
                    ld.getMealType() != null ? ld.getMealType().toUpperCase() : "LUNCH");
        } catch (IllegalArgumentException e) {
            mealType = EventMealRegistration.MealType.LUNCH;
        }

        EventMealRegistration reg = mealRepo
                .findByEventIdAndUserIdAndMealDateAndMealType(ld.getMainEventId(), user.getId(), ld.getDate(), mealType)
                .orElseThrow(() -> new ResourceNotFoundException("Meal registration not found", lunchDinnerId));

        reg.setHeadCount(headCount > 0 ? headCount : 1);
        mealRepo.save(reg);
        return getUserMeals(ld.getMainEventId(), user.getId());
    }

    /** Enforce target_plates capacity against event_meal_registrations only. */
    private void checkMealCapacity(Long eventId, LocalDate date,
                                    EventMealRegistration.MealType mealType, int requested, Long userId) {
        lunchDinnerRepo.findByEventAndDateAndType(eventId, date, mealType.name()).ifPresent(config -> {
            int target = config.getTargetPlates() != null && config.getTargetPlates() > 0
                    ? config.getTargetPlates() : Integer.MAX_VALUE;
            int alreadyBooked = mealRepo.sumHeadCountExcludingUser(eventId, date, mealType, userId);
            if (alreadyBooked + requested > target) {
                throw new EventFullException(
                        config.getName() + " (Target: " + target + ", Booked: " + alreadyBooked + ")", target);
            }
        });
    }
}
