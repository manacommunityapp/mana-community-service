package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.*;
import com.manacommunity.api.events.entity.*;
import com.manacommunity.api.events.repository.*;
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
public class EventProgramService {

    private final CommunityEventRepository eventRepo;
    private final EventProgramRepository programRepo;
    private final ActivityRegistrationRepository activityRegRepo;
    private final MealRegistrationRepository mealRegRepo;

    private CommunityEvent findEvent(Long eventId) {
        return eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    // ── Programs CRUD ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventProgramResponse> getPrograms(Long eventId, String dayLabel) {
        List<EventProgram> programs = dayLabel != null
                ? programRepo.findByEventIdAndDayLabelOrderBySortOrderAsc(eventId, dayLabel)
                : programRepo.findByEventIdOrderBySortOrderAsc(eventId);
        return programs.stream().map(this::toProgramResponse).toList();
    }

    @Transactional
    public EventProgramResponse createProgram(EventProgramRequest req) {
        CommunityEvent event = findEvent(req.getEventId());
        EventProgram p = EventProgram.builder()
                .event(event)
                .title(req.getTitle())
                .dayLabel(req.getDayLabel())
                .dayDate(req.getDayDate())
                .programType(req.getProgramType())
                .activityType(req.getActivityType() != null ? req.getActivityType() : "OPEN")
                .startTime(req.getStartTime())
                .duration(req.getDuration())
                .venue(req.getVenue())
                .performer(req.getPerformer())
                .judge(req.getJudge())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .capacity(req.getCapacity())
                .requiresRegistration(Boolean.TRUE.equals(req.getRequiresRegistration()))
                .waitlistEnabled(Boolean.TRUE.equals(req.getWaitlistEnabled()))
                .registeredCount(0)
                .slotStatus("OPEN")
                .build();
        return toProgramResponse(programRepo.save(p));
    }

    @Transactional
    public EventProgramResponse updateProgram(Long id, EventProgramRequest req) {
        EventProgram p = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventProgram", id));
        p.setTitle(req.getTitle());
        p.setDayLabel(req.getDayLabel());
        p.setDayDate(req.getDayDate());
        p.setProgramType(req.getProgramType());
        if (req.getActivityType() != null) p.setActivityType(req.getActivityType());
        p.setStartTime(req.getStartTime());
        p.setDuration(req.getDuration());
        p.setVenue(req.getVenue());
        p.setPerformer(req.getPerformer());
        p.setJudge(req.getJudge());
        if (req.getSortOrder() != null) p.setSortOrder(req.getSortOrder());
        if (req.getCapacity() != null) p.setCapacity(req.getCapacity());
        if (req.getRequiresRegistration() != null) p.setRequiresRegistration(req.getRequiresRegistration());
        if (req.getWaitlistEnabled() != null) p.setWaitlistEnabled(req.getWaitlistEnabled());
        return toProgramResponse(programRepo.save(p));
    }

    @Transactional
    public void deleteProgram(Long id) {
        programRepo.deleteById(id);
    }

    private EventProgramResponse toProgramResponse(EventProgram p) {
        EventProgramResponse r = new EventProgramResponse();
        r.setId(p.getId());
        r.setEventId(p.getEvent().getId());
        r.setEventTitle(p.getEvent().getTitle());
        r.setDayLabel(p.getDayLabel());
        r.setDayDate(p.getDayDate() != null ? p.getDayDate().toString() : null);
        r.setTitle(p.getTitle());
        r.setProgramType(p.getProgramType());
        r.setActivityType(p.getActivityType());
        r.setStartTime(p.getStartTime());
        r.setDuration(p.getDuration());
        r.setVenue(p.getVenue());
        r.setPerformer(p.getPerformer());
        r.setJudge(p.getJudge());
        r.setSortOrder(p.getSortOrder() != null ? p.getSortOrder() : 0);
        r.setCapacity(p.getCapacity());
        r.setRequiresRegistration(Boolean.TRUE.equals(p.getRequiresRegistration()));
        r.setRegisteredCount(p.getRegisteredCount() != null ? p.getRegisteredCount() : 0);
        r.setSpotsLeft(p.spotsLeft());
        r.setSlotStatus(p.getSlotStatus());
        r.setWaitlistEnabled(Boolean.TRUE.equals(p.getWaitlistEnabled()));
        r.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return r;
    }

    // ── Activity Registration ────────────────────────────────────────────────────

    @Transactional
    public ActivityRegistrationResponse joinActivity(Long programId, AppUser user, ActivityRegistrationRequest req) {
        EventProgram program = programRepo.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("EventProgram", programId));

        if (!Boolean.TRUE.equals(program.getRequiresRegistration())) {
            throw new IllegalStateException("This activity does not require registration");
        }

        if (activityRegRepo.existsByProgramIdAndUserId(programId, user.getId())) {
            throw new IllegalStateException("Already registered for this activity");
        }

        int heads = req != null && req.getHeadCount() != null && req.getHeadCount() > 0
                ? req.getHeadCount() : 1;

        ActivityRegistration.ActivityRegStatus status;
        if (program.isFull()) {
            if (Boolean.TRUE.equals(program.getWaitlistEnabled())) {
                status = ActivityRegistration.ActivityRegStatus.WAITLISTED;
            } else {
                throw new IllegalStateException("Activity is full");
            }
        } else if (program.getCapacity() != null && program.spotsLeft() < heads) {
            if (Boolean.TRUE.equals(program.getWaitlistEnabled())) {
                status = ActivityRegistration.ActivityRegStatus.WAITLISTED;
            } else {
                throw new IllegalStateException("Not enough spots available (need " + heads + ", have " + program.spotsLeft() + ")");
            }
        } else {
            status = ActivityRegistration.ActivityRegStatus.CONFIRMED;
        }

        ActivityRegistration reg = ActivityRegistration.builder()
                .program(program)
                .user(user)
                .headCount(heads)
                .status(status)
                .build();
        reg = activityRegRepo.save(reg);

        if (status == ActivityRegistration.ActivityRegStatus.CONFIRMED) {
            program.setRegisteredCount((program.getRegisteredCount() != null ? program.getRegisteredCount() : 0) + heads);
            if (program.isFull()) {
                program.setSlotStatus("FULL");
            }
            programRepo.save(program);
        }

        return toActivityRegResponse(reg);
    }

    @Transactional
    public void leaveActivity(Long programId, AppUser user) {
        ActivityRegistration reg = activityRegRepo.findByProgramIdAndUserId(programId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ActivityRegistration", "program+user", programId + "+" + user.getId()));

        if (reg.getStatus() == ActivityRegistration.ActivityRegStatus.CONFIRMED) {
            EventProgram program = reg.getProgram();
            program.setRegisteredCount(Math.max(0,
                    (program.getRegisteredCount() != null ? program.getRegisteredCount() : 0) - reg.getHeadCount()));
            if ("FULL".equals(program.getSlotStatus()) && !program.isFull()) {
                program.setSlotStatus("OPEN");
            }
            programRepo.save(program);
        }

        activityRegRepo.delete(reg);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationResponse> getActivityRegistrations(Long programId) {
        return activityRegRepo.findByProgramIdOrderByRegisteredAtDesc(programId)
                .stream().map(this::toActivityRegResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationResponse> getUserActivityRegistrations(Long userId) {
        return activityRegRepo.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream().map(this::toActivityRegResponse).toList();
    }

    private ActivityRegistrationResponse toActivityRegResponse(ActivityRegistration r) {
        ActivityRegistrationResponse resp = new ActivityRegistrationResponse();
        resp.setId(r.getId());
        resp.setProgramId(r.getProgram().getId());
        resp.setProgramTitle(r.getProgram().getTitle());
        resp.setUserId(r.getUser().getId());
        resp.setUserName(r.getUser().getFullName());
        resp.setHeadCount(r.getHeadCount() != null ? r.getHeadCount() : 1);
        resp.setStatus(r.getStatus().name());
        resp.setSpotsLeft(r.getProgram().spotsLeft());
        resp.setRegisteredAt(r.getRegisteredAt() != null ? r.getRegisteredAt().toString() : null);
        return resp;
    }

    // ── Meal Registration ────────────────────────────────────────────────────────

    @Transactional
    public MealRegistrationResponse saveMeals(AppUser user, MealRegistrationRequest req) {
        CommunityEvent event = findEvent(req.getEventId());

        mealRegRepo.deleteByEventIdAndUserId(event.getId(), user.getId());

        MealRegistration.DietaryPref pref = MealRegistration.DietaryPref.VEG;
        if (req.getDietaryPref() != null) {
            try { pref = MealRegistration.DietaryPref.valueOf(req.getDietaryPref().toUpperCase()); } catch (Exception ignored) {}
        }

        List<MealRegistration> saved = new ArrayList<>();
        if (req.getMeals() != null) {
            for (MealRegistrationRequest.DayMeal dm : req.getMeals()) {
                int heads = dm.getHeadCount() > 0 ? dm.getHeadCount() : 1;
                if (dm.isLunch()) {
                    saved.add(mealRegRepo.save(MealRegistration.builder()
                            .event(event).user(user).mealDate(dm.getDate())
                            .mealType(MealRegistration.MealType.LUNCH)
                            .headCount(heads).dietaryPref(pref).allergies(req.getAllergies())
                            .build()));
                }
                if (dm.isDinner()) {
                    saved.add(mealRegRepo.save(MealRegistration.builder()
                            .event(event).user(user).mealDate(dm.getDate())
                            .mealType(MealRegistration.MealType.DINNER)
                            .headCount(heads).dietaryPref(pref).allergies(req.getAllergies())
                            .build()));
                }
            }
        }

        return toMealResponse(event.getId(), user.getId(), saved);
    }

    @Transactional(readOnly = true)
    public MealRegistrationResponse getUserMeals(Long eventId, Long userId) {
        List<MealRegistration> meals = mealRegRepo.findByEventIdAndUserId(eventId, userId);
        return toMealResponse(eventId, userId, meals);
    }

    @Transactional(readOnly = true)
    public MealSummaryResponse getMealSummary(Long eventId) {
        List<MealRegistration> all = mealRegRepo.findByEventIdOrdered(eventId);

        Map<LocalDate, Map<MealRegistration.MealType, MealSummaryResponse.MealBreakdown>> grouped = new TreeMap<>();

        for (MealRegistration m : all) {
            grouped
                    .computeIfAbsent(m.getMealDate(), k -> new EnumMap<>(MealRegistration.MealType.class))
                    .compute(m.getMealType(), (type, bd) -> {
                        if (bd == null) bd = new MealSummaryResponse.MealBreakdown();
                        int h = m.getHeadCount() != null ? m.getHeadCount() : 1;
                        bd.setTotalHeads(bd.getTotalHeads() + h);
                        switch (m.getDietaryPref() != null ? m.getDietaryPref() : MealRegistration.DietaryPref.VEG) {
                            case VEG -> bd.setVeg(bd.getVeg() + h);
                            case VEGAN -> bd.setVegan(bd.getVegan() + h);
                            case JAIN -> bd.setJain(bd.getJain() + h);
                            case NONVEG -> bd.setNonveg(bd.getNonveg() + h);
                        }
                        return bd;
                    });
        }

        MealSummaryResponse resp = new MealSummaryResponse();
        resp.setEventId(eventId);
        List<MealSummaryResponse.DaySummary> days = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            MealSummaryResponse.DaySummary ds = new MealSummaryResponse.DaySummary();
            ds.setDate(entry.getKey().toString());
            ds.setLunch(entry.getValue().getOrDefault(MealRegistration.MealType.LUNCH, new MealSummaryResponse.MealBreakdown()));
            ds.setDinner(entry.getValue().getOrDefault(MealRegistration.MealType.DINNER, new MealSummaryResponse.MealBreakdown()));
            days.add(ds);
        }
        resp.setDays(days);
        return resp;
    }

    private MealRegistrationResponse toMealResponse(Long eventId, Long userId, List<MealRegistration> meals) {
        MealRegistrationResponse resp = new MealRegistrationResponse();
        resp.setEventId(eventId);
        resp.setUserId(userId);

        if (!meals.isEmpty()) {
            MealRegistration first = meals.get(0);
            resp.setDietaryPref(first.getDietaryPref() != null ? first.getDietaryPref().name() : null);
            resp.setAllergies(first.getAllergies());
        }

        Map<LocalDate, MealRegistrationResponse.DayMealResponse> dayMap = new TreeMap<>();
        for (MealRegistration m : meals) {
            MealRegistrationResponse.DayMealResponse dm = dayMap.computeIfAbsent(m.getMealDate(), d -> {
                MealRegistrationResponse.DayMealResponse r = new MealRegistrationResponse.DayMealResponse();
                r.setDate(d.toString());
                r.setHeadCount(m.getHeadCount() != null ? m.getHeadCount() : 1);
                return r;
            });
            if (m.getMealType() == MealRegistration.MealType.LUNCH) dm.setLunch(true);
            if (m.getMealType() == MealRegistration.MealType.DINNER) dm.setDinner(true);
        }
        resp.setMeals(new ArrayList<>(dayMap.values()));
        return resp;
    }
}
