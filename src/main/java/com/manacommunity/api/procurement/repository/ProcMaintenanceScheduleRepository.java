package com.manacommunity.api.procurement.repository;

import com.manacommunity.api.procurement.entity.ProcMaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProcMaintenanceScheduleRepository extends JpaRepository<ProcMaintenanceSchedule, Long> {

    List<ProcMaintenanceSchedule> findByCommunityIdAndScheduledDateBetween(Long communityId, LocalDate start, LocalDate end);

    List<ProcMaintenanceSchedule> findByAmcContractIdAndStatus(Long amcContractId, ProcMaintenanceSchedule.ScheduleStatus status);

    @Query("SELECT s FROM ProcMaintenanceSchedule s WHERE s.communityId = :communityId AND s.status = 'UPCOMING' AND s.scheduledDate < :today")
    List<ProcMaintenanceSchedule> findOverdueSchedules(Long communityId, LocalDate today);

    List<ProcMaintenanceSchedule> findByCommunityIdAndAssetIdentifier(Long communityId, String assetIdentifier);
}
