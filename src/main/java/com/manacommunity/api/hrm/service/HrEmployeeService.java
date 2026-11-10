package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrEmployeeService {

    private final HrEmployeeRepository employeeRepository;
    private final HrOrganizationRepository organizationRepository;
    private final HrBranchRepository branchRepository;
    private final HrDepartmentRepository departmentRepository;
    private final HrDesignationRepository designationRepository;
    private final HrEmployeeDocumentRepository documentRepository;

    @Transactional
    public EmployeeResponse createEmployee(Long orgId, Long communityId, CreateEmployeeRequest request, Long createdBy) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(orgId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (employeeRepository.existsByOrganizationIdAndManaUserId(orgId, request.getManaUserId())) {
            throw new IllegalArgumentException("User is already an employee in this organization");
        }

        String employeeCode = request.getEmployeeCode();
        if (employeeCode == null || employeeCode.isBlank()) {
            long count = employeeRepository.countByOrganizationId(orgId);
            employeeCode = "EMP-" + String.format("%04d", count + 1);
        }

        if (employeeRepository.existsByOrganizationIdAndEmployeeCode(orgId, employeeCode)) {
            throw new IllegalArgumentException("Employee code already exists in this organization");
        }

        HrEmployee employee = HrEmployee.builder()
                .organization(org)
                .manaUserId(request.getManaUserId())
                .employeeCode(employeeCode)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .personalEmail(request.getPersonalEmail())
                .workEmail(request.getWorkEmail())
                .phone(request.getPhone())
                .employmentType(request.getEmploymentType())
                .joiningDate(request.getJoiningDate())
                .reportingManagerId(request.getReportingManagerId())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();

        if (request.getBranchId() != null) {
            employee.setBranch(branchRepository.findById(request.getBranchId()).orElse(null));
        }
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(request.getDepartmentId()).orElse(null));
        }
        if (request.getDesignationId() != null) {
            employee.setDesignation(designationRepository.findById(request.getDesignationId()).orElse(null));
        }

        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long orgId, Long employeeId, UpdateEmployeeRequest request, Long updatedBy) {
        HrEmployee employee = employeeRepository.findByIdAndOrganizationId(employeeId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (request.getVersion() != null && !request.getVersion().equals(employee.getVersion())) {
            throw new IllegalStateException("Employee record has been modified by another user");
        }

        if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
        if (request.getLastName() != null) employee.setLastName(request.getLastName());
        if (request.getPersonalEmail() != null) employee.setPersonalEmail(request.getPersonalEmail());
        if (request.getWorkEmail() != null) employee.setWorkEmail(request.getWorkEmail());
        if (request.getPhone() != null) employee.setPhone(request.getPhone());
        if (request.getEmploymentType() != null) employee.setEmploymentType(request.getEmploymentType());
        if (request.getEmploymentStatus() != null) employee.setEmploymentStatus(request.getEmploymentStatus());
        if (request.getJoiningDate() != null) employee.setJoiningDate(request.getJoiningDate());
        if (request.getConfirmationDate() != null) employee.setConfirmationDate(request.getConfirmationDate());
        if (request.getExitDate() != null) employee.setExitDate(request.getExitDate());
        if (request.getReportingManagerId() != null) employee.setReportingManagerId(request.getReportingManagerId());
        if (request.getEmergencyContactName() != null) employee.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) employee.setEmergencyContactPhone(request.getEmergencyContactPhone());

        if (request.getBranchId() != null) {
            employee.setBranch(branchRepository.findById(request.getBranchId()).orElse(null));
        }
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(request.getDepartmentId()).orElse(null));
        }
        if (request.getDesignationId() != null) {
            employee.setDesignation(designationRepository.findById(request.getDesignationId()).orElse(null));
        }

        employee.setUpdatedBy(updatedBy);
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long orgId, Long employeeId) {
        HrEmployee employee = employeeRepository.findByIdAndOrganizationId(employeeId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeListResponse> listEmployees(Long orgId, EmploymentStatus status, Pageable pageable) {
        Page<HrEmployee> page;
        if (status != null) {
            page = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, status, pageable);
        } else {
            page = employeeRepository.findByOrganizationId(orgId, pageable);
        }
        return page.map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public List<HrEmployee> getEmployeesByManaUserId(Long manaUserId) {
        return employeeRepository.findByManaUserId(manaUserId);
    }

    @Transactional(readOnly = true)
    public HrEmployee findByOrgAndManaUser(Long orgId, Long manaUserId) {
        return employeeRepository.findByOrganizationIdAndManaUserId(orgId, manaUserId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found for this user in this organization"));
    }

    @Transactional
    public void uploadDocument(Long orgId, Long employeeId, UploadDocumentRequest request, Long uploadedBy) {
        HrEmployee employee = employeeRepository.findByIdAndOrganizationId(employeeId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        HrEmployeeDocument doc = HrEmployeeDocument.builder()
                .organizationId(orgId)
                .employee(employee)
                .documentType(request.getDocumentType())
                .documentName(request.getDocumentName())
                .fileUrl(request.getFileUrl())
                .fileKey(request.getFileKey())
                .expiryDate(request.getExpiryDate())
                .uploadedBy(uploadedBy)
                .build();
        documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<HrEmployeeDocument> listDocuments(Long employeeId) {
        return documentRepository.findByEmployeeId(employeeId);
    }

    private EmployeeResponse toResponse(HrEmployee emp) {
        String reportingManagerName = null;
        if (emp.getReportingManagerId() != null) {
            reportingManagerName = employeeRepository.findById(emp.getReportingManagerId())
                    .map(m -> m.getFirstName() + " " + (m.getLastName() != null ? m.getLastName() : ""))
                    .orElse(null);
        }

        return EmployeeResponse.builder()
                .id(emp.getId())
                .organizationId(emp.getOrganization().getId())
                .employeeCode(emp.getEmployeeCode())
                .firstName(emp.getFirstName())
                .lastName(emp.getLastName())
                .personalEmail(emp.getPersonalEmail())
                .workEmail(emp.getWorkEmail())
                .phone(emp.getPhone())
                .employmentType(emp.getEmploymentType())
                .employmentStatus(emp.getEmploymentStatus())
                .joiningDate(emp.getJoiningDate())
                .confirmationDate(emp.getConfirmationDate())
                .exitDate(emp.getExitDate())
                .manaUserId(emp.getManaUserId())
                .branchId(emp.getBranch() != null ? emp.getBranch().getId() : null)
                .branchName(emp.getBranch() != null ? emp.getBranch().getBranchName() : null)
                .departmentId(emp.getDepartment() != null ? emp.getDepartment().getId() : null)
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getDepartmentName() : null)
                .designationId(emp.getDesignation() != null ? emp.getDesignation().getId() : null)
                .designationName(emp.getDesignation() != null ? emp.getDesignation().getDesignationName() : null)
                .reportingManagerId(emp.getReportingManagerId())
                .reportingManagerName(reportingManagerName)
                .emergencyContactName(emp.getEmergencyContactName())
                .emergencyContactPhone(emp.getEmergencyContactPhone())
                .version(emp.getVersion())
                .createdAt(emp.getCreatedAt())
                .updatedAt(emp.getUpdatedAt())
                .build();
    }

    private EmployeeListResponse toListResponse(HrEmployee emp) {
        return EmployeeListResponse.builder()
                .id(emp.getId())
                .employeeCode(emp.getEmployeeCode())
                .firstName(emp.getFirstName())
                .lastName(emp.getLastName())
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getDepartmentName() : null)
                .designationName(emp.getDesignation() != null ? emp.getDesignation().getDesignationName() : null)
                .employmentStatus(emp.getEmploymentStatus())
                .joiningDate(emp.getJoiningDate())
                .build();
    }
}
