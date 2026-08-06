package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceMaintenance;
import com.manacommunity.api.booking.entity.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceMaintenanceRepository extends JpaRepository<ResourceMaintenance, Long> {

    List<ResourceMaintenance> findByResourceIdAndStatusInOrderByStartDateAsc(Long resourceId, List<MaintenanceStatus> statuses);

    List<ResourceMaintenance> findByCommunityIdOrderByStartDateDesc(Long communityId);

    @Query("SELECT m FROM ResourceMaintenance m WHERE m.resource.id = :resourceId " +
           "AND m.startDate < :end AND m.endDate > :start")
    List<ResourceMaintenance> findOverlapping(
            @Param("resourceId") Long resourceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
