package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HrAssetAssignmentRepository extends JpaRepository<HrAssetAssignment, Long> {
    List<HrAssetAssignment> findByEmployeeIdAndIsActiveTrue(Long employeeId);
    Optional<HrAssetAssignment> findByAssetIdAndIsActiveTrue(Long assetId);
    List<HrAssetAssignment> findByOrganizationId(Long orgId);
}
