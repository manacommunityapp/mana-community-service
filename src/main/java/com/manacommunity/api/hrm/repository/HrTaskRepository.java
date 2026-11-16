package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HrTaskRepository extends JpaRepository<HrTask, Long> {
    List<HrTask> findByAssignedToId(Long employeeId);
    List<HrTask> findByAssignedToIdAndStatus(Long employeeId, String status);
    List<HrTask> findByOrganizationIdAndStatus(Long orgId, String status);
}
