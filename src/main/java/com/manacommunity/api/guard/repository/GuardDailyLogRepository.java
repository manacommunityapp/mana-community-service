package com.manacommunity.api.guard.repository;

import com.manacommunity.api.guard.entity.GuardDailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GuardDailyLogRepository extends JpaRepository<GuardDailyLog, Long> {
    List<GuardDailyLog> findByCommunityIdAndLogDateOrderByCreatedAtDesc(Long communityId, LocalDate logDate);
    List<GuardDailyLog> findByCommunityIdOrderByLogDateDescCreatedAtDesc(Long communityId);
}
