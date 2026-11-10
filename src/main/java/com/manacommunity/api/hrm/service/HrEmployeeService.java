package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrEmployeeService {

    private final HrEmployeeRepository employeeRepository;
    private final HrOrganizationRepository organizationRepository;
    private final HrBranchRepository branchRepository;
    private final HrDepartmentRepository departmentRepository;
    private final HrDesignationRepository designationRepository;
    private final HrEmployeeDocumentRepository documentRepository;
    private final HrEmploymentHistoryRepository historyRepository;
    private final HrOnboardingService onboardingService;
    private final HrOffboardingService offboardingService;

    @Transactional
    public EmployeeResponse createEmployee(Long orgId, Long communityId, CreateEmployeeRequest request, Long createdBy) {
        HrOrganization org = organizationRepository.findByIdAndCommunityId(orgId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (employeeRepository.existsByOrganizationIdAndManaUserId(orgId, request.getManaUserId())) {
            throw new IllegalArgumentException("User is already an employee in this organization");
        }

        HrEmployee employee = HrEmployee.builder()
                .organization(org)
                .manaUserId(request.getManaUserId())
                .employeeCode("TBD")
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
            employee.setBranch(branchRepository.findByIdAndOrganizationId(request.getBranchId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found in this organization")));
        }
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findByIdAndOrganizationId(request.getDepartmentId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found in this organization")));
        }
        if (request.getDesignationId() != null) {
            employee.setDesignation(designationRepository.findByIdAndOrganizationId(request.getDesignationId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Designation not found in this organization")));
        }

        String requestedCode = request.getEmployeeCode();
        HrEmployee savedEmployee;
        if (requestedCode != null && !requestedCode.isBlank()) {
            if (employeeRepository.existsByOrganizationIdAndEmployeeCode(orgId, requestedCode)) {
                throw new IllegalArgumentException("Employee code already exists in this organization");
            }
            employee.setEmployeeCode(requestedCode);
            savedEmployee = employeeRepository.save(employee);
        } else {
            // Auto-generate with retry to handle concurrent creates
            HrEmployee result = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    long count = employeeRepository.countByOrganizationId(orgId);
                    employee.setEmployeeCode("EMP-" + String.format("%04d", count + 1 + attempt));
                    result = employeeRepository.save(employee);
                    break;
                } catch (DataIntegrityViolationException e) {
                    if (attempt == 2) throw new IllegalStateException("Failed to generate unique employee code. Please try again.");
                }
            }
            if (result == null) throw new IllegalStateException("Failed to generate unique employee code");
            savedEmployee = result;
        }

        // Record joining history
        historyRepository.save(HrEmploymentHistory.builder()
            .organizationId(orgId)
            .employee(savedEmployee)
            .changeType("JOINING")
            .effectiveDate(savedEmployee.getJoiningDate() != null ? savedEmployee.getJoiningDate() : LocalDate.now())
            .newDesignation(savedEmployee.getDesignation() != null ? savedEmployee.getDesignation().getDesignationName() : null)
            .newDepartment(savedEmployee.getDepartment() != null ? savedEmployee.getDepartment().getDepartmentName() : null)
            .changedBy(createdBy)
            .build());

        // Initiate onboarding tasks
        onboardingService.initiateOnboarding(savedEmployee);

        return toResponse(savedEmployee);
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

        // Track changes for employment history
        String prevDesignation = employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null;
        String prevDepartment = employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null;
        String prevBranch = employee.getBranch() != null ? employee.getBranch().getBranchName() : null;
        Long prevManagerId = employee.getReportingManagerId();
        boolean exitDateBeingSet = request.getExitDate() != null && employee.getExitDate() == null;

        if (request.getBranchId() != null) {
            employee.setBranch(branchRepository.findByIdAndOrganizationId(request.getBranchId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found in this organization")));
        }
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findByIdAndOrganizationId(request.getDepartmentId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found in this organization")));
        }
        if (request.getDesignationId() != null) {
            employee.setDesignation(designationRepository.findByIdAndOrganizationId(request.getDesignationId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Designation not found in this organization")));
        }

        employee.setUpdatedBy(updatedBy);
        employee = employeeRepository.save(employee);

        // Record designation change history
        String newDesignationName = employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null;
        if (!Objects.equals(prevDesignation, newDesignationName)) {
            historyRepository.save(HrEmploymentHistory.builder()
                .organizationId(orgId).employee(employee).changeType("DESIGNATION_CHANGE")
                .effectiveDate(LocalDate.now()).previousDesignation(prevDesignation)
                .newDesignation(newDesignationName).changedBy(updatedBy).build());
        }
        // Record department change history
        String newDepartmentName = employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null;
        if (!Objects.equals(prevDepartment, newDepartmentName)) {
            historyRepository.save(HrEmploymentHistory.builder()
                .organizationId(orgId).employee(employee).changeType("DEPARTMENT_CHANGE")
                .effectiveDate(LocalDate.now()).previousDepartment(prevDepartment)
                .newDepartment(newDepartmentName).changedBy(updatedBy).build());
        }
        // Record branch change history
        String newBranchName = employee.getBranch() != null ? employee.getBranch().getBranchName() : null;
        if (!Objects.equals(prevBranch, newBranchName)) {
            historyRepository.save(HrEmploymentHistory.builder()
                .organizationId(orgId).employee(employee).changeType("TRANSFER")
                .effectiveDate(LocalDate.now()).previousBranch(prevBranch)
                .newBranch(newBranchName).changedBy(updatedBy).build());
        }
        // Record manager change history
        if (!Objects.equals(prevManagerId, employee.getReportingManagerId())) {
            historyRepository.save(HrEmploymentHistory.builder()
                .organizationId(orgId).employee(employee).changeType("REPORTING_MANAGER_CHANGE")
                .effectiveDate(LocalDate.now()).previousReportingManagerId(prevManagerId)
                .newReportingManagerId(employee.getReportingManagerId()).changedBy(updatedBy).build());
        }

        // Trigger offboarding when exitDate is set for the first time
        if (exitDateBeingSet) {
            offboardingService.initiateOffboarding(employee);
        }

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

    @Transactional(readOnly = true)
    public HrEmployee resolveAndVerifyMembership(Long orgId, Long manaUserId) {
        return employeeRepository.findByOrganizationIdAndManaUserId(orgId, manaUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this organization"));
    }

    public EmployeeResponse toEmployeeResponse(HrEmployee employee) {
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse selfServiceUpdate(Long employeeId, SelfServiceUpdateRequest request) {
        HrEmployee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));
        if (request.getPersonalEmail() != null) employee.setPersonalEmail(request.getPersonalEmail());
        if (request.getPhone() != null) employee.setPhone(request.getPhone());
        if (request.getEmergencyContactName() != null) employee.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) employee.setEmergencyContactPhone(request.getEmergencyContactPhone());
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmploymentHistoryResponse> getEmploymentHistory(Long orgId, Long employeeId) {
        employeeRepository.findByIdAndOrganizationId(employeeId, orgId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found in this organization"));
        return historyRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId).stream()
            .map(h -> EmploymentHistoryResponse.builder()
                .id(h.getId()).changeType(h.getChangeType()).effectiveDate(h.getEffectiveDate())
                .previousDesignation(h.getPreviousDesignation()).newDesignation(h.getNewDesignation())
                .previousDepartment(h.getPreviousDepartment()).newDepartment(h.getNewDepartment())
                .previousBranch(h.getPreviousBranch()).newBranch(h.getNewBranch())
                .previousReportingManagerId(h.getPreviousReportingManagerId())
                .newReportingManagerId(h.getNewReportingManagerId())
                .reason(h.getReason()).remarks(h.getRemarks())
                .changedBy(h.getChangedBy()).createdAt(h.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrgHierarchyNode> getOrgHierarchy(Long orgId) {
        List<HrEmployee> roots = employeeRepository.findRootEmployeesByOrg(orgId);
        return roots.stream().map(this::buildHierarchyNode).collect(Collectors.toList());
    }

    private OrgHierarchyNode buildHierarchyNode(HrEmployee employee) {
        List<HrEmployee> reports = employeeRepository.findDirectReports(employee.getId());
        return OrgHierarchyNode.builder()
            .id(employee.getId())
            .employeeCode(employee.getEmployeeCode())
            .firstName(employee.getFirstName())
            .lastName(employee.getLastName())
            .designation(employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null)
            .department(employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null)
            .directReports(reports.stream().map(this::buildHierarchyNode).collect(Collectors.toList()))
            .build();
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
