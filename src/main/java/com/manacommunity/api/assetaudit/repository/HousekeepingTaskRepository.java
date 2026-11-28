package com.manacommunity.api.assetaudit.repository;

import com.manacommunity.api.assetaudit.entity.HousekeepingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

    Optional<HousekeepingTask> findByQrCode(String qrCode);

    List<HousekeepingTask> findByAssignedStaffId(String staffId);

    @Query("SELECT t FROM HousekeepingTask t WHERE t.status IN ('REPORTED_DIRTY', 'ESCALATED') ORDER BY t.priority DESC, t.residentReportedAt ASC")
    List<HousekeepingTask> findPendingUrgentTasks();

    @Query("SELECT t FROM HousekeepingTask t WHERE t.escalated = true AND t.status != 'COMPLETED' ORDER BY t.escalationLevel DESC")
    List<HousekeepingTask> findActiveEscalations();

    List<HousekeepingTask> findByTowerNameAndStatus(String towerName, HousekeepingTask.TaskStatus status);

    @Query("SELECT t FROM HousekeepingTask t WHERE t.communityId = :communityId AND t.status NOT IN ('COMPLETED', 'VERIFIED', 'SKIPPED') ORDER BY t.priority DESC")
    List<HousekeepingTask> findActiveTasks(Long communityId);
}
