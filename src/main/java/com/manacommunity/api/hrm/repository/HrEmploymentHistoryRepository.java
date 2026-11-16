package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrEmploymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrEmploymentHistoryRepository extends JpaRepository<HrEmploymentHistory, Long> {
    List<HrEmploymentHistory> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
    List<HrEmploymentHistory> findByOrganizationIdOrderByEffectiveDateDesc(Long organizationId);
}
