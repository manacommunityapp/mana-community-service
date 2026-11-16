package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrEmployeeRole;
import com.manacommunity.api.hrm.enums.HrmRole;
import com.manacommunity.api.hrm.repository.HrEmployeeRepository;
import com.manacommunity.api.hrm.repository.HrEmployeeRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrRbacService {

    private final HrEmployeeRoleRepository roleRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrEmployeeRole grantRole(Long orgId, Long employeeId, HrmRole role, Long grantedBy) {
        if (roleRepository.existsByOrganizationIdAndEmployeeIdAndRoleAndIsActiveTrue(orgId, employeeId, role)) {
            return roleRepository.findByOrganizationIdAndEmployeeIdAndRoleAndIsActiveTrue(orgId, employeeId, role).get();
        }
        HrEmployee employee = employeeRepository.findByIdAndOrganizationId(employeeId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found in org"));
        HrEmployeeRole hrRole = HrEmployeeRole.builder()
                .organizationId(orgId)
                .employee(employee)
                .role(role)
                .grantedBy(grantedBy)
                .isActive(true)
                .build();
        return roleRepository.save(hrRole);
    }

    @Transactional
    public void revokeRole(Long orgId, Long roleId) {
        HrEmployeeRole role = roleRepository.findById(roleId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Role assignment not found"));
        role.setIsActive(false);
        roleRepository.save(role);
    }

    public List<HrEmployeeRole> getRoles(Long orgId, Long employeeId) {
        return roleRepository.findByOrganizationIdAndEmployeeIdAndIsActiveTrue(orgId, employeeId);
    }

    public boolean hasRole(Long orgId, Long employeeId, HrmRole role) {
        return roleRepository.existsByOrganizationIdAndEmployeeIdAndRoleAndIsActiveTrue(orgId, employeeId, role);
    }

    public List<HrEmployeeRole> getAllRoleAssignments(Long orgId) {
        return roleRepository.findByOrganizationIdAndIsActiveTrue(orgId);
    }
}
