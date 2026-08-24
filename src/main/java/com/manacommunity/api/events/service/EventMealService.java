package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.MealRegistrationRequest;
import com.manacommunity.api.events.dto.MealRegistrationResponse;
import com.manacommunity.api.events.dto.MealSummaryResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventMealRegistration;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventMealRegistrationRepository;
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

            boolean lunch = entry.getValue().stream().anyMatch(m -> m.getMealType() == EventMealRegistration.MealType.LUNCH);
            boolean dinner = entry.getValue().stream().anyMatch(m -> m.getMealType() == EventMealRegistration.MealType.DINNER);
            int headCount = entry.getValue().stream().mapToInt(m -> m.getHeadCount() != null ? m.getHeadCount() : 1).max().orElse(1);

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
            for (MealRegistrationRequest.DayMeal dm : req.getMeals()) {
                if (dm.getDate() == null) continue;

                Long communityId = event.getCommunity() != null ? event.getCommunity().getId() : null;

                if (dm.isLunch()) {
                    EventMealRegistration lunchReg = EventMealRegistration.builder()
                            .event(event)
                            .user(user)
                            .communityId(communityId)
                            .mealDate(dm.getDate())
                            .mealType(EventMealRegistration.MealType.LUNCH)
                            .headCount(dm.getHeadCount() > 0 ? dm.getHeadCount() : 1)
                            .dietaryPref(dietaryPref)
                            .allergies(req.getAllergies())
                            .build();
                    mealRepo.save(lunchReg);
                }

                if (dm.isDinner()) {
                    EventMealRegistration dinnerReg = EventMealRegistration.builder()
                            .event(event)
                            .user(user)
                            .communityId(communityId)
                            .mealDate(dm.getDate())
                            .mealType(EventMealRegistration.MealType.DINNER)
                            .headCount(dm.getHeadCount() > 0 ? dm.getHeadCount() : 1)
                            .dietaryPref(dietaryPref)
                            .allergies(req.getAllergies())
                            .build();
                    mealRepo.save(dinnerReg);
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

            MealSummaryResponse.MealBreakdown lunch = new MealSummaryResponse.MealBreakdown();
            MealSummaryResponse.MealBreakdown dinner = new MealSummaryResponse.MealBreakdown();

            for (EventMealRegistration m : entry.getValue()) {
                int hc = m.getHeadCount() != null ? m.getHeadCount() : 1;
                EventMealRegistration.DietaryPref pref = m.getDietaryPref() != null ? m.getDietaryPref() : EventMealRegistration.DietaryPref.VEG;

                if (m.getMealType() == EventMealRegistration.MealType.LUNCH) {
                    lunch.setTotalHeads(lunch.getTotalHeads() + hc);
                    switch (pref) {
                        case VEG -> lunch.setVeg(lunch.getVeg() + hc);
                        case VEGAN -> lunch.setVegan(lunch.getVegan() + hc);
                        case JAIN -> lunch.setJain(lunch.getJain() + hc);
                        case NONVEG -> lunch.setNonveg(lunch.getNonveg() + hc);
                    }
                } else if (m.getMealType() == EventMealRegistration.MealType.DINNER) {
                    dinner.setTotalHeads(dinner.getTotalHeads() + hc);
                    switch (pref) {
                        case VEG -> dinner.setVeg(dinner.getVeg() + hc);
                        case VEGAN -> dinner.setVegan(dinner.getVegan() + hc);
                        case JAIN -> dinner.setJain(dinner.getJain() + hc);
                        case NONVEG -> dinner.setNonveg(dinner.getNonveg() + hc);
                    }
                }
            }

            ds.setLunch(lunch);
            ds.setDinner(dinner);
            days.add(ds);
        }

        response.setDays(days);
        return response;
    }
}
