package com.manacommunity.api.maintenance.repository;

import com.manacommunity.api.maintenance.entity.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {
    List<MaintenanceSchedule> findByCommunityIdAndIsActiveTrueOrderByNextDueDateAsc(Long communityId);

    List<MaintenanceSchedule> findByIsActiveTrueAndNextDueDateLessThanEqual(java.time.LocalDate date);
}
