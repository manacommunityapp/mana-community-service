package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwOperationalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MwOperationalAssignmentRepository extends JpaRepository<MwOperationalAssignment, Long> {

    List<MwOperationalAssignment> findByOrganizationIdOrderByAssignedAtDesc(Long orgId);

    List<MwOperationalAssignment> findByOrganizationIdAndStatusOrderByAssignedAtDesc(Long orgId, String status);

    List<MwOperationalAssignment> findByOrganizationIdAndSourceModuleOrderByAssignedAtDesc(Long orgId, String sourceModule);

    List<MwOperationalAssignment> findByEmployeeIdOrderByAssignedAtDesc(Long employeeId);

    Optional<MwOperationalAssignment> findBySourceModuleAndSourceRefId(String sourceModule, Long sourceRefId);
}
