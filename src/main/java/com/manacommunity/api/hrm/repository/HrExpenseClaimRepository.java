package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrExpenseClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrExpenseClaimRepository extends JpaRepository<HrExpenseClaim, Long> {
    Page<HrExpenseClaim> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<HrExpenseClaim> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
}
