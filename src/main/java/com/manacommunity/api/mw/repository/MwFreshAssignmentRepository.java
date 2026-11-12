package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwFreshAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MwFreshAssignmentRepository extends JpaRepository<MwFreshAssignment, Long> {

    Optional<MwFreshAssignment> findByFreshOrderId(String freshOrderId);

    Optional<MwFreshAssignment> findByOperationalAssignment_Id(Long opAssignmentId);

    @Query("SELECT fa FROM MwFreshAssignment fa WHERE fa.operationalAssignment.organizationId = :orgId ORDER BY fa.createdAt DESC")
    List<MwFreshAssignment> findByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT fa FROM MwFreshAssignment fa WHERE fa.operationalAssignment.employeeId = :employeeId ORDER BY fa.createdAt DESC")
    List<MwFreshAssignment> findByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT fa FROM MwFreshAssignment fa WHERE fa.operationalAssignment.organizationId = :orgId AND fa.operationalAssignment.employeeId = :employeeId ORDER BY fa.createdAt DESC")
    List<MwFreshAssignment> findByOrganizationIdAndEmployeeId(@Param("orgId") Long orgId, @Param("employeeId") Long employeeId);
}
