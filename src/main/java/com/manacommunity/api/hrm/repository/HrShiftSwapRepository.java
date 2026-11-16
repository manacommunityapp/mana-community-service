package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrShiftSwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrShiftSwapRepository extends JpaRepository<HrShiftSwapRequest, Long> {
    List<HrShiftSwapRequest> findByRequesterEmployeeId(Long employeeId);
    List<HrShiftSwapRequest> findByTargetEmployeeIdAndStatus(Long employeeId, String status);
    List<HrShiftSwapRequest> findByOrganizationIdAndStatus(Long orgId, String status);
}
