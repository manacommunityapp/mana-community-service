package com.manacommunity.platform.privacy;

import com.manacommunity.platform.privacy.HealthDataAccessLogger.HealthDataAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthDataAccessLogRepository extends JpaRepository<HealthDataAccessLog, Long> {
    List<HealthDataAccessLog> findBySubjectUserIdOrderByAccessedAtDesc(Long subjectUserId);
}
