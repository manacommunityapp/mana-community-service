package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrComplianceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HrComplianceAlertRepository extends JpaRepository<HrComplianceAlert, Long> {

    List<HrComplianceAlert> findByOrganizationIdAndStatusOrderByDueDateAsc(Long orgId, String status);

    List<HrComplianceAlert> findByOrganizationIdOrderByDueDateAsc(Long orgId);

    boolean existsByOrganizationIdAndEmployeeIdAndAlertTypeAndDueDate(Long orgId, Long employeeId, String alertType, LocalDate dueDate);
}
