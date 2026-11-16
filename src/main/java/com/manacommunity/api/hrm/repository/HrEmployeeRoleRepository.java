package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrEmployeeRole;
import com.manacommunity.api.hrm.enums.HrmRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrEmployeeRoleRepository extends JpaRepository<HrEmployeeRole, Long> {

    List<HrEmployeeRole> findByOrganizationIdAndEmployeeIdAndIsActiveTrue(Long orgId, Long employeeId);

    List<HrEmployeeRole> findByOrganizationIdAndIsActiveTrue(Long orgId);

    Optional<HrEmployeeRole> findByOrganizationIdAndEmployeeIdAndRoleAndIsActiveTrue(Long orgId, Long employeeId, HrmRole role);

    boolean existsByOrganizationIdAndEmployeeIdAndRoleAndIsActiveTrue(Long orgId, Long employeeId, HrmRole role);
}
