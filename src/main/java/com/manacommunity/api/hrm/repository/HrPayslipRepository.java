package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrPayslip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HrPayslipRepository extends JpaRepository<HrPayslip, Long> {
    List<HrPayslip> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<HrPayslip> findByPayrollRunId(Long payrollRunId);
    Optional<HrPayslip> findByEmployeeIdAndPayrollRunId(Long employeeId, Long payrollRunId);
    List<HrPayslip> findByEmployeeIdAndIsPublishedTrue(Long employeeId);
}
