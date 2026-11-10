package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrDepartmentRepository extends JpaRepository<HrDepartment, Long> {

    List<HrDepartment> findByOrganizationId(Long organizationId);

    Optional<HrDepartment> findByIdAndOrganizationId(Long id, Long organizationId);
}
