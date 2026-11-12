package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrOffboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrOffboardingTaskRepository extends JpaRepository<HrOffboardingTask, Long> {
    List<HrOffboardingTask> findByEmployeeIdOrderByDueDateAsc(Long employeeId);
    List<HrOffboardingTask> findByOrganizationIdAndIsCompletedFalse(Long organizationId);
}
