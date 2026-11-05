package com.manacommunity.api.wellness.service;

import com.manacommunity.api.wellness.dto.ExerciseCatalogDto;
import com.manacommunity.api.wellness.dto.WorkoutLogDto;
import com.manacommunity.api.wellness.entity.ExerciseCatalog;
import com.manacommunity.api.wellness.entity.WorkoutLog;
import com.manacommunity.api.wellness.entity.WellnessProfile;
import com.manacommunity.api.wellness.repository.ExerciseCatalogRepository;
import com.manacommunity.api.wellness.repository.WorkoutLogRepository;
import com.manacommunity.api.wellness.repository.WellnessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private static final BigDecimal DEFAULT_WEIGHT_KG = new BigDecimal("70");

    private final WorkoutLogRepository workoutLogRepository;
    private final ExerciseCatalogRepository exerciseCatalogRepository;
    private final WellnessProfileRepository profileRepository;

    // ── Workout Logging ──

    public List<WorkoutLogDto> getWorkoutsForDate(Long userId, LocalDate date) {
        return workoutLogRepository.findByUserIdAndWorkoutDateOrderByStartTimeDesc(userId, date)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<WorkoutLogDto> getWorkoutsForRange(Long userId, LocalDate start, LocalDate end) {
        return workoutLogRepository.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDateDescStartTimeDesc(userId, start, end)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public WorkoutLogDto logWorkout(Long userId, WorkoutLogDto dto) {
        BigDecimal userWeight = profileRepository.findByUserId(userId)
                .map(p -> p.getWeightKg() != null ? p.getWeightKg() : DEFAULT_WEIGHT_KG)
                .orElse(DEFAULT_WEIGHT_KG);

        ExerciseCatalog exercise = dto.getExerciseCatalogId() != null
                ? exerciseCatalogRepository.findById(dto.getExerciseCatalogId()).orElse(null)
                : null;

        Integer caloriesBurned = dto.getCaloriesBurned();
        if (caloriesBurned == null && exercise != null && dto.getDurationMin() != null && dto.getDurationMin() > 0) {
            caloriesBurned = computeCaloriesBurned(exercise.getMetValue(), userWeight, dto.getDurationMin());
        }

        WorkoutLog log = WorkoutLog.builder()
                .userId(userId)
                .exerciseCatalogId(dto.getExerciseCatalogId())
                .exerciseName(exercise != null ? exercise.getName() : dto.getExerciseName())
                .workoutDate(dto.getWorkoutDate() != null ? dto.getWorkoutDate() : LocalDate.now())
                .startTime(dto.getStartTime() != null ? dto.getStartTime() : LocalTime.now())
                .durationMin(dto.getDurationMin())
                .caloriesBurned(caloriesBurned)
                .sets(dto.getSets())
                .repsPerSet(dto.getRepsPerSet())
                .weightKg(dto.getWeightKg())
                .distanceKm(dto.getDistanceKm())
                .source(dto.getSource() != null ? dto.getSource() : "MANUAL")
                .notes(dto.getNotes())
                .build();

        return toDto(workoutLogRepository.save(log));
    }

    @Transactional
    public void deleteWorkout(Long userId, Long workoutId) {
        workoutLogRepository.findById(workoutId)
                .filter(w -> w.getUserId().equals(userId))
                .ifPresent(workoutLogRepository::delete);
    }

    // ── Exercise Catalog ──

    public List<ExerciseCatalogDto> searchExercises(String query) {
        if (query == null || query.isBlank()) {
            return exerciseCatalogRepository.findAll().stream()
                    .map(this::toCatalogDto).collect(Collectors.toList());
        }
        return exerciseCatalogRepository.findByNameContainingIgnoreCaseOrNameLocalContainingIgnoreCase(query, query)
                .stream().map(this::toCatalogDto).collect(Collectors.toList());
    }

    public List<ExerciseCatalogDto> getExercisesByCategory(String category) {
        return exerciseCatalogRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::toCatalogDto).collect(Collectors.toList());
    }

    // ── Calorie computation ──

    public Integer getTotalCaloriesBurnedForDate(Long userId, LocalDate date) {
        return workoutLogRepository.getTotalCaloriesBurnedForDate(userId, date);
    }

    public Integer getWorkoutCountForDate(Long userId, LocalDate date) {
        return workoutLogRepository.getWorkoutCountForDate(userId, date);
    }

    // ── MET formula: kcal = MET × weight_kg × (duration_min / 60) ──
    private int computeCaloriesBurned(BigDecimal met, BigDecimal weightKg, int durationMin) {
        BigDecimal hours = new BigDecimal(durationMin).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
        return met.multiply(weightKg).multiply(hours).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    // ── Mappers ──

    private WorkoutLogDto toDto(WorkoutLog w) {
        ExerciseCatalog ex = w.getExerciseCatalogId() != null
                ? exerciseCatalogRepository.findById(w.getExerciseCatalogId()).orElse(null)
                : null;
        return WorkoutLogDto.builder()
                .id(w.getId())
                .userId(w.getUserId())
                .exerciseCatalogId(w.getExerciseCatalogId())
                .exerciseName(w.getExerciseName())
                .exerciseCategory(ex != null ? ex.getCategory() : null)
                .workoutDate(w.getWorkoutDate())
                .startTime(w.getStartTime())
                .durationMin(w.getDurationMin())
                .caloriesBurned(w.getCaloriesBurned())
                .sets(w.getSets())
                .repsPerSet(w.getRepsPerSet())
                .weightKg(w.getWeightKg())
                .distanceKm(w.getDistanceKm())
                .source(w.getSource())
                .notes(w.getNotes())
                .metValue(ex != null ? ex.getMetValue() : null)
                .build();
    }

    private ExerciseCatalogDto toCatalogDto(ExerciseCatalog e) {
        return ExerciseCatalogDto.builder()
                .id(e.getId())
                .name(e.getName())
                .nameLocal(e.getNameLocal())
                .category(e.getCategory())
                .description(e.getDescription())
                .metValue(e.getMetValue())
                .musclesTargeted(e.getMusclesTargeted())
                .isEquipmentNeeded(e.getIsEquipmentNeeded())
                .isBodyweight(e.getIsBodyweight())
                .verified(e.getVerified())
                .build();
    }
}
