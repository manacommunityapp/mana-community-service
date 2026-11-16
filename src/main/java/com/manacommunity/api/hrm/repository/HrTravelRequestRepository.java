package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrTravelRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrTravelRequestRepository extends JpaRepository<HrTravelRequest, Long> {
    Page<HrTravelRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<HrTravelRequest> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
}
