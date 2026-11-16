package com.manacommunity.api.wellness.service;

import com.manacommunity.api.wellness.dto.ActivityGoalDto;
import com.manacommunity.api.wellness.dto.BodyMetricLogDto;
import com.manacommunity.api.wellness.dto.FitnessStreakDto;
import com.manacommunity.api.wellness.entity.*;
import com.manacommunity.api.wellness.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FitnessGoalService {

    private final BodyMetricLogRepository bodyMetricLogRepository;
    private final ActivityGoalRepository activityGoalRepository;
    private final FitnessStreakRepository fitnessStreakRepository;
    private final WorkoutLogRepository workoutLogRepository;
    private final StepLogRepository stepLogRepository;
    private final WellnessProfileRepository profileRepository;

    // ── Body Metrics ──────────────────────────────────────────

    public List<BodyMetricLogDto> getBodyMetrics(Long userId, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        return bodyMetricLogRepository.findByUserIdAndLoggedDateBetweenOrderByLoggedDateAsc(userId, from, LocalDate.now())
                .stream().map(this::toMetricDto).collect(Collectors.toList());
    }

    @Transactional
    public BodyMetricLogDto logBodyMetric(Long userId, BodyMetricLogDto dto) {
        LocalDate date = dto.getLoggedDate() != null ? dto.getLoggedDate() : LocalDate.now();

        BodyMetricLog log = bodyMetricLogRepository.findByUserIdAndLoggedDate(userId, date)
                .orElse(BodyMetricLog.builder().userId(userId).loggedDate(date).createdAt(LocalDateTime.now()).build());

        if (dto.getWeightKg() != null) log.setWeightKg(dto.getWeightKg());
        if (dto.getBodyFatPct() != null) log.setBodyFatPct(dto.getBodyFatPct());
        if (dto.getMuscleMassKg() != null) log.setMuscleMassKg(dto.getMuscleMassKg());
        if (dto.getWaistCm() != null) log.setWaistCm(dto.getWaistCm());
        if (dto.getChestCm() != null) log.setChestCm(dto.getChestCm());
        if (dto.getHipCm() != null) log.setHipCm(dto.getHipCm());
        if (dto.getNotes() != null) log.setNotes(dto.getNotes());
        log.setSource(dto.getSource() != null ? dto.getSource() : "MANUAL");

        // Compute BMI if we have weight and profile has height
        if (log.getWeightKg() != null) {
            profileRepository.findByUserId(userId).ifPresent(profile -> {
                if (profile.getHeightCm() != null && profile.getHeightCm().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    BigDecimal bmi = log.getWeightKg().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
                    log.setBmi(bmi);
                }
            });
        }

        return toMetricDto(bodyMetricLogRepository.save(log));
    }

    public BodyMetricLogDto getLatestBodyMetric(Long userId) {
        return bodyMetricLogRepository.findFirstByUserIdOrderByLoggedDateDesc(userId)
                .map(this::toMetricDto)
                .orElse(BodyMetricLogDto.builder().userId(userId).build());
    }

    // ── Activity Goals ────────────────────────────────────────

    public ActivityGoalDto getActivityGoal(Long userId) {
        return activityGoalRepository.findByUserId(userId)
                .map(this::toGoalDto)
                .orElse(ActivityGoalDto.builder()
                        .userId(userId)
                        .dailyStepsTarget(8000)
                        .activeMinutesTarget(30)
                        .weeklyWorkoutsTarget(4)
                        .build());
    }

    @Transactional
    public ActivityGoalDto saveActivityGoal(Long userId, ActivityGoalDto dto) {
        ActivityGoal goal = activityGoalRepository.findByUserId(userId)
                .orElse(ActivityGoal.builder().userId(userId).createdAt(LocalDateTime.now()).build());
        if (dto.getDailyStepsTarget() != null) goal.setDailyStepsTarget(dto.getDailyStepsTarget());
        if (dto.getActiveMinutesTarget() != null) goal.setActiveMinutesTarget(dto.getActiveMinutesTarget());
        if (dto.getWeeklyWorkoutsTarget() != null) goal.setWeeklyWorkoutsTarget(dto.getWeeklyWorkoutsTarget());
        goal.setUpdatedAt(LocalDateTime.now());
        return toGoalDto(activityGoalRepository.save(goal));
    }

    // ── Streak ────────────────────────────────────────────────

    public FitnessStreakDto getStreak(Long userId) {
        FitnessStreak streak = fitnessStreakRepository.findByUserId(userId)
                .orElse(FitnessStreak.builder().userId(userId).currentStreakDays(0).longestStreakDays(0).totalActiveDays(0).build());

        ActivityGoal goal = activityGoalRepository.findByUserId(userId).orElse(null);
        int weeklyTarget = goal != null && goal.getWeeklyWorkoutsTarget() != null ? goal.getWeeklyWorkoutsTarget() : 4;
        int stepGoal = goal != null && goal.getDailyStepsTarget() != null ? goal.getDailyStepsTarget() : 8000;

        // Count workouts this week (Mon–Sun)
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int weeklyCompleted = workoutLogRepository.getWorkoutCountForDateRange(userId, weekStart, LocalDate.now());

        // Today's steps
        Integer todaySteps = stepLogRepository.getTotalStepsForDate(userId, LocalDate.now());

        return FitnessStreakDto.builder()
                .userId(userId)
                .currentStreakDays(streak.getCurrentStreakDays())
                .longestStreakDays(streak.getLongestStreakDays())
                .lastActivityDate(streak.getLastActivityDate())
                .totalActiveDays(streak.getTotalActiveDays())
                .weeklyWorkoutsCompleted(weeklyCompleted)
                .weeklyWorkoutsTarget(weeklyTarget)
                .todaySteps(todaySteps != null ? todaySteps : 0)
                .stepGoal(stepGoal)
                .build();
    }

    @Transactional
    public void refreshStreak(Long userId) {
        // Active days = union of workout days + days where step count met goal
        List<LocalDate> workoutDates = workoutLogRepository.findDistinctWorkoutDatesByUserId(userId);

        ActivityGoal goal = activityGoalRepository.findByUserId(userId).orElse(null);
        int stepGoal = goal != null && goal.getDailyStepsTarget() != null ? goal.getDailyStepsTarget() : 8000;
        List<LocalDate> stepGoalDates = stepLogRepository.findDatesWhereStepsMetGoal(userId, stepGoal);

        // Sorted descending union
        TreeSet<LocalDate> activeDateSet = new TreeSet<>((a, b) -> b.compareTo(a));
        activeDateSet.addAll(workoutDates);
        activeDateSet.addAll(stepGoalDates);
        List<LocalDate> activeDates = new ArrayList<>(activeDateSet);

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        for (LocalDate d : activeDates) {
            if (!d.isAfter(cursor) && !d.isBefore(cursor.minusDays(1))) {
                streak++;
                cursor = d.minusDays(1);
            } else {
                break;
            }
        }

        FitnessStreak fs = fitnessStreakRepository.findByUserId(userId)
                .orElse(FitnessStreak.builder().userId(userId).longestStreakDays(0).totalActiveDays(0).build());
        fs.setCurrentStreakDays(streak);
        fs.setLongestStreakDays(Math.max(fs.getLongestStreakDays() != null ? fs.getLongestStreakDays() : 0, streak));
        fs.setTotalActiveDays(activeDates.size());
        fs.setLastActivityDate(activeDates.isEmpty() ? null : activeDates.get(0));
        fs.setUpdatedAt(LocalDateTime.now());
        fitnessStreakRepository.save(fs);
    }

    // ── Auto-log body metric when WellnessProfile weight changes ─
    @Transactional
    public void autoLogWeightFromProfile(Long userId, BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null) return;
        BodyMetricLogDto dto = BodyMetricLogDto.builder()
                .weightKg(weightKg)
                .source("PROFILE_UPDATE")
                .build();
        logBodyMetric(userId, dto);
    }

    // ── Mappers ──────────────────────────────────────────────

    private BodyMetricLogDto toMetricDto(BodyMetricLog m) {
        return BodyMetricLogDto.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .loggedDate(m.getLoggedDate())
                .weightKg(m.getWeightKg())
                .bodyFatPct(m.getBodyFatPct())
                .muscleMassKg(m.getMuscleMassKg())
                .bmi(m.getBmi())
                .waistCm(m.getWaistCm())
                .chestCm(m.getChestCm())
                .hipCm(m.getHipCm())
                .source(m.getSource())
                .notes(m.getNotes())
                .build();
    }

    private ActivityGoalDto toGoalDto(ActivityGoal g) {
        return ActivityGoalDto.builder()
                .id(g.getId())
                .userId(g.getUserId())
                .dailyStepsTarget(g.getDailyStepsTarget())
                .activeMinutesTarget(g.getActiveMinutesTarget())
                .weeklyWorkoutsTarget(g.getWeeklyWorkoutsTarget())
                .build();
    }
}
