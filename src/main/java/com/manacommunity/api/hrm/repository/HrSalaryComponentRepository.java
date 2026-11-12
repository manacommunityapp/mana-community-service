package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrSalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HrSalaryComponentRepository extends JpaRepository<HrSalaryComponent, Long> {
    List<HrSalaryComponent> findByOrganizationIdAndIsActiveTrueOrderBySequenceOrder(Long orgId);
}
