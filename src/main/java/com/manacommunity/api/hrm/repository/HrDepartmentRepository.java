package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrDepartmentRepository extends JpaRepository<HrDepartment, Long> {

    List<HrDepartment> findByOrganizationId(Long organizationId);
}
