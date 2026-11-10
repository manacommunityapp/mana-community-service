package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrOnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrOnboardingTaskRepository extends JpaRepository<HrOnboardingTask, Long> {
    List<HrOnboardingTask> findByEmployeeIdOrderByDueDateAsc(Long employeeId);
    List<HrOnboardingTask> findByOrganizationIdAndIsCompletedFalse(Long organizationId);
}
