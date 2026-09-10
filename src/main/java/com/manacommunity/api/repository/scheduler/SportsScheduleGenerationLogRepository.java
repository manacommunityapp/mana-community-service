package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsScheduleGenerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsScheduleGenerationLogRepository extends JpaRepository<SportsScheduleGenerationLog, Long> {

    List<SportsScheduleGenerationLog> findByConfigIdOrderByCreatedAtDesc(Long configId);

    List<SportsScheduleGenerationLog> findByEventIdOrderByCreatedAtDesc(Long eventId);

    Page<SportsScheduleGenerationLog> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<SportsScheduleGenerationLog> findByGeneratedByOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
