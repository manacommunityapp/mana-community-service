package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.MealRegistrationRequest;
import com.manacommunity.api.events.dto.MealRegistrationResponse;
import com.manacommunity.api.events.dto.MealSummaryResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.MealRegistration;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.MealRegistrationRepository;
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

    private final MealRegistrationRepository mealRepo;
    private final CommunityEventRepository eventRepo;

    @Transactional(readOnly = true)
    public MealRegistrationResponse getUserMeals(Long eventId, Long userId) {
        List<MealRegistration> list = mealRepo.findByEventIdAndUserId(eventId, userId);

        MealRegistrationResponse response = new MealRegistrationResponse();
        response.setEventId(eventId);
        response.setUserId(userId);

        if (list.isEmpty()) {
            response.setMeals(Collections.emptyList());
            return response;
        }

        MealRegistration first = list.get(0);
        response.setDietaryPref(first.getDietaryPref() != null ? first.getDietaryPref().name() : "VEG");
        response.setAllergies(first.getAllergies());

        // Group by mealDate
        Map<LocalDate, List<MealRegistration>> grouped = list.stream()
                .collect(Collectors.groupingBy(MealRegistration::getMealDate, TreeMap::new, Collectors.toList()));

        List<MealRegistrationResponse.DayMealResponse> dayMeals = new ArrayList<>();
        for (Map.Entry<LocalDate, List<MealRegistration>> entry : grouped.entrySet()) {
            MealRegistrationResponse.DayMealResponse dmr = new MealRegistrationResponse.DayMealResponse();
            dmr.setDate(entry.getKey().toString());

            boolean lunch = entry.getValue().stream().anyMatch(m -> m.getMealType() == MealRegistration.MealType.LUNCH);
            boolean dinner = entry.getValue().stream().anyMatch(m -> m.getMealType() == MealRegistration.MealType.DINNER);
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
        CommunityEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        mealRepo.deleteByEventIdAndUserId(eventId, user.getId());

        MealRegistration.DietaryPref dietaryPref = MealRegistration.DietaryPref.VEG;
        if (req.getDietaryPref() != null && !req.getDietaryPref().isBlank()) {
            try {
                dietaryPref = MealRegistration.DietaryPref.valueOf(req.getDietaryPref().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        if (req.getMeals() != null) {
            for (MealRegistrationRequest.DayMeal dm : req.getMeals()) {
                if (dm.getDate() == null) continue;

                if (dm.isLunch()) {
                    MealRegistration lunchReg = MealRegistration.builder()
                            .event(event)
                            .user(user)
                            .mealDate(dm.getDate())
                            .mealType(MealRegistration.MealType.LUNCH)
                            .headCount(dm.getHeadCount() > 0 ? dm.getHeadCount() : 1)
                            .dietaryPref(dietaryPref)
                            .allergies(req.getAllergies())
                            .build();
                    mealRepo.save(lunchReg);
                }

                if (dm.isDinner()) {
                    MealRegistration dinnerReg = MealRegistration.builder()
                            .event(event)
                            .user(user)
                            .mealDate(dm.getDate())
                            .mealType(MealRegistration.MealType.DINNER)
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
        List<MealRegistration> list = mealRepo.findByEventIdOrdered(eventId);

        MealSummaryResponse response = new MealSummaryResponse();
        response.setEventId(eventId);

        // Group by mealDate
        Map<LocalDate, List<MealRegistration>> grouped = list.stream()
                .collect(Collectors.groupingBy(MealRegistration::getMealDate, TreeMap::new, Collectors.toList()));

        List<MealSummaryResponse.DaySummary> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<MealRegistration>> entry : grouped.entrySet()) {
            MealSummaryResponse.DaySummary ds = new MealSummaryResponse.DaySummary();
            ds.setDate(entry.getKey().toString());

            MealSummaryResponse.MealBreakdown lunch = new MealSummaryResponse.MealBreakdown();
            MealSummaryResponse.MealBreakdown dinner = new MealSummaryResponse.MealBreakdown();

            for (MealRegistration m : entry.getValue()) {
                int hc = m.getHeadCount() != null ? m.getHeadCount() : 1;
                MealRegistration.DietaryPref pref = m.getDietaryPref() != null ? m.getDietaryPref() : MealRegistration.DietaryPref.VEG;

                if (m.getMealType() == MealRegistration.MealType.LUNCH) {
                    lunch.setTotalHeads(lunch.getTotalHeads() + hc);
                    switch (pref) {
                        case VEG -> lunch.setVeg(lunch.getVeg() + hc);
                        case VEGAN -> lunch.setVegan(lunch.getVegan() + hc);
                        case JAIN -> lunch.setJain(lunch.getJain() + hc);
                        case NONVEG -> lunch.setNonveg(lunch.getNonveg() + hc);
                    }
                } else if (m.getMealType() == MealRegistration.MealType.DINNER) {
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
