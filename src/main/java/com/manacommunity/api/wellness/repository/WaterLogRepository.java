package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.WaterLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaterLogRepository extends JpaRepository<WaterLog, Long> {
    List<WaterLog> findByUserIdAndLogDateOrderByLogTimeDesc(Long userId, LocalDate logDate);

    @Query("SELECT COALESCE(SUM(w.amountMl), 0) FROM WaterLog w WHERE w.userId = :userId AND w.logDate = :logDate")
    Integer getTotalWaterForDate(Long userId, LocalDate logDate);
}
