package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {
    List<WorkoutLog> findByUserIdAndWorkoutDateOrderByStartTimeDesc(Long userId, LocalDate workoutDate);
    List<WorkoutLog> findByUserIdAndWorkoutDateBetweenOrderByWorkoutDateDescStartTimeDesc(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(w.caloriesBurned), 0) FROM WorkoutLog w WHERE w.userId = :userId AND w.workoutDate = :date")
    Integer getTotalCaloriesBurnedForDate(Long userId, LocalDate date);

    @Query("SELECT COUNT(w) FROM WorkoutLog w WHERE w.userId = :userId AND w.workoutDate = :date")
    Integer getWorkoutCountForDate(Long userId, LocalDate date);
}
