package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrPayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HrPayrollRunRepository extends JpaRepository<HrPayrollRun, Long> {
    Optional<HrPayrollRun> findByOrganizationIdAndPayrollMonthAndPayrollYear(Long orgId, int month, int year);
    List<HrPayrollRun> findByOrganizationIdOrderByPayrollYearDescPayrollMonthDesc(Long orgId);
}
