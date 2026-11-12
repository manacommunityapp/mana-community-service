package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwDutyHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MwDutyHandoverRepository extends JpaRepository<MwDutyHandover, Long> {

    List<MwDutyHandover> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);

    @Query("SELECT h FROM MwDutyHandover h WHERE h.organizationId = :orgId AND FUNCTION('DATE', h.createdAt) = :date ORDER BY h.createdAt DESC")
    List<MwDutyHandover> findByOrganizationIdAndDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    List<MwDutyHandover> findByHandoverToIdAndAcknowledgedFalse(Long handoverToId);

    List<MwDutyHandover> findByOrganizationIdAndHandoverToIdAndAcknowledgedFalse(Long orgId, Long handoverToId);

    @Query("SELECT h FROM MwDutyHandover h WHERE h.acknowledged = false AND h.createdAt < :cutoff")
    List<MwDutyHandover> findUnacknowledgedBefore(@Param("cutoff") LocalDateTime cutoff);
}
