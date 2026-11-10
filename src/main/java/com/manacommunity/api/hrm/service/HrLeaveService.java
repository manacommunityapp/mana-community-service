package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrLeaveService {

    private final HrLeaveRequestRepository leaveRequestRepository;
    private final HrLeaveBalanceRepository leaveBalanceRepository;
    private final HrLeaveTypeRepository leaveTypeRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public LeaveRequestResponse applyLeave(HrEmployee employee, ApplyLeaveRequest request) {
        HrLeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));

        if (!leaveType.getOrganization().getId().equals(employee.getOrganization().getId())) {
            throw new IllegalArgumentException("Leave type does not belong to this organization");
        }

        // Half-day validation
        boolean isHalfDay = Boolean.TRUE.equals(request.getHalfDay());
        if (isHalfDay) {
            if (request.getLeaveSession() == null) {
                throw new IllegalArgumentException("Leave session (FIRST_HALF/SECOND_HALF) is required for half-day leave");
            }
            if (!request.getFromDate().equals(request.getToDate())) {
                throw new IllegalArgumentException("Half-day leave must be on a single date");
            }
            request.setTotalDays(0.5);
        }

        // Policy: min notice days
        if (leaveType.getMinNoticeDays() != null && leaveType.getMinNoticeDays() > 0) {
            LocalDate minApplyDate = request.getFromDate().minusDays(leaveType.getMinNoticeDays());
            if (LocalDate.now().isAfter(minApplyDate)) {
                throw new IllegalArgumentException(
                        "This leave type requires " + leaveType.getMinNoticeDays() + " days advance notice");
            }
        }

        // Policy: max consecutive days
        if (leaveType.getMaxConsecutiveDays() != null) {
            if (request.getTotalDays() > leaveType.getMaxConsecutiveDays()) {
                throw new IllegalArgumentException(
                        "Cannot apply more than " + leaveType.getMaxConsecutiveDays() + " consecutive days for this leave type");
            }
        }

        List<HrLeaveRequest> overlapping = leaveRequestRepository.findOverlapping(
                employee.getId(), request.getFromDate(), request.getToDate());
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Overlapping leave request exists for the selected dates");
        }

        int year = request.getFromDate().getYear();
        HrLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), request.getLeaveTypeId(), year)
                .orElse(null);

        if (balance == null) {
            throw new IllegalStateException("No leave balance configured. Please contact HR to initialize your leave balance.");
        }
        if (!Boolean.TRUE.equals(leaveType.getAllowNegativeBalance())
                && balance.getRemainingBalance() < request.getTotalDays()) {
            throw new IllegalArgumentException("Insufficient leave balance");
        }

        HrLeaveRequest leaveRequest = HrLeaveRequest.builder()
                .organizationId(employee.getOrganization().getId())
                .employee(employee)
                .leaveType(leaveType)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .totalDays(request.getTotalDays())
                .reason(request.getReason())
                .isHalfDay(isHalfDay)
                .leaveSession(request.getLeaveSession())
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse approveLeave(Long id, Long orgId, Long approvedBy, LeaveActionRequest request) {
        HrLeaveRequest leaveRequest = leaveRequestRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        LeaveApprovalStatus status = leaveRequest.getApprovalStatus();
        if (status != LeaveApprovalStatus.PENDING && status != LeaveApprovalStatus.L1_APPROVED) {
            throw new IllegalStateException("Leave request is not in an approvable status");
        }

        HrLeaveType leaveType = leaveRequest.getLeaveType();

        // Two-level approval: if leave type requires 2 levels and still at L1
        if (leaveType.getApprovalLevels() != null && leaveType.getApprovalLevels() == 2
                && status == LeaveApprovalStatus.PENDING) {
            leaveRequest.setApprovalStatus(LeaveApprovalStatus.L1_APPROVED);
            leaveRequest.setL1ApprovedBy(approvedBy);
            leaveRequest.setL1ApprovedAt(LocalDateTime.now());
            leaveRequest.setL1Remarks(request.getApproverRemarks());
            return toResponse(leaveRequestRepository.save(leaveRequest));
        }

        // Final approval (single-level or L2)
        leaveRequest.setApprovalStatus(LeaveApprovalStatus.APPROVED);
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApproverRemarks(request.getApproverRemarks());
        leaveRequest.setApprovedAt(LocalDateTime.now());
        HrLeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        int year = savedRequest.getFromDate().getYear();
        HrEmployee emp = savedRequest.getEmployee();
        HrLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(emp.getId(), leaveType.getId(), year)
                .orElseGet(() -> HrLeaveBalance.builder()
                        .organizationId(orgId)
                        .employee(emp)
                        .leaveType(leaveType)
                        .year(year)
                        .totalBalance((double) leaveType.getDefaultBalance())
                        .usedBalance(0.0)
                        .remainingBalance((double) leaveType.getDefaultBalance())
                        .build());

        balance.setUsedBalance(balance.getUsedBalance() + savedRequest.getTotalDays());
        balance.setRemainingBalance(balance.getTotalBalance() - balance.getUsedBalance());
        leaveBalanceRepository.save(balance);

        return toResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse rejectLeave(Long id, Long orgId, Long rejectedBy, LeaveActionRequest request) {
        HrLeaveRequest leaveRequest = leaveRequestRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        LeaveApprovalStatus status = leaveRequest.getApprovalStatus();
        if (status != LeaveApprovalStatus.PENDING && status != LeaveApprovalStatus.L1_APPROVED) {
            throw new IllegalStateException("Leave request is not in a rejectable status");
        }

        // If rejecting at L1 stage, mark as L1_REJECTED
        LeaveApprovalStatus newStatus = (status == LeaveApprovalStatus.PENDING)
                ? LeaveApprovalStatus.REJECTED
                : LeaveApprovalStatus.REJECTED; // final rejection regardless of level

        leaveRequest.setApprovalStatus(newStatus);
        leaveRequest.setApprovedBy(rejectedBy);
        leaveRequest.setApproverRemarks(request.getApproverRemarks());
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public void grantCompensatoryOff(Long orgId, Long employeeId, LocalDate workedDate, String reason) {
        HrLeaveType compOffType = leaveTypeRepository
                .findByOrganizationIdAndLeaveTypeNameIgnoreCase(orgId, "Compensatory Off")
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Compensatory Off leave type not configured for this organization"));

        int year = workedDate.getYear();
        HrEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));

        HrLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, compOffType.getId(), year)
                .orElseGet(() -> HrLeaveBalance.builder()
                        .organizationId(orgId)
                        .employee(employee)
                        .leaveType(compOffType)
                        .year(year)
                        .totalBalance(0.0)
                        .usedBalance(0.0)
                        .remainingBalance(0.0)
                        .build());

        balance.setTotalBalance(balance.getTotalBalance() + 1.0);
        balance.setRemainingBalance(balance.getRemainingBalance() + 1.0);
        leaveBalanceRepository.save(balance);
    }

    @Transactional
    public LeaveRequestResponse cancelLeave(Long id, Long employeeId) {
        HrLeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new IllegalArgumentException("You can only cancel your own leave requests");
        }

        if (leaveRequest.getApprovalStatus() != LeaveApprovalStatus.PENDING
                && leaveRequest.getApprovalStatus() != LeaveApprovalStatus.APPROVED) {
            throw new IllegalStateException("Only PENDING or APPROVED leave requests can be cancelled");
        }

        if (leaveRequest.getApprovalStatus() == LeaveApprovalStatus.APPROVED) {
            int year = leaveRequest.getFromDate().getYear();
            HrLeaveBalance balance = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(
                            leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                    .orElseThrow(() -> new IllegalStateException("Leave balance not found"));
            balance.setUsedBalance(balance.getUsedBalance() - leaveRequest.getTotalDays());
            balance.setRemainingBalance(balance.getTotalBalance() - balance.getUsedBalance());
            leaveBalanceRepository.save(balance);
        }

        leaveRequest.setApprovalStatus(LeaveApprovalStatus.CANCELLED);
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalance(Long employeeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .stream()
                .map(b -> LeaveBalanceResponse.builder()
                        .leaveTypeId(b.getLeaveType().getId())
                        .leaveTypeName(b.getLeaveType().getLeaveTypeName())
                        .totalBalance(b.getTotalBalance())
                        .usedBalance(b.getUsedBalance())
                        .remainingBalance(b.getRemainingBalance())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getMyLeaveRequests(Long employeeId, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getOrgLeaveRequests(Long orgId, LeaveApprovalStatus status, Pageable pageable) {
        return leaveRequestRepository.findByOrganizationIdAndApprovalStatus(orgId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public HrLeaveType createLeaveType(HrOrganization org, CreateLeaveTypeRequest request) {
        HrLeaveType leaveType = HrLeaveType.builder()
                .organization(org)
                .leaveTypeName(request.getLeaveTypeName())
                .defaultBalance(request.getDefaultBalance())
                .isPaid(request.getIsPaid() != null ? request.getIsPaid() : true)
                .isCarryForward(request.getIsCarryForward() != null ? request.getIsCarryForward() : false)
                .maxCarryForward(request.getMaxCarryForward())
                .approvalLevels(request.getApprovalLevels() != null ? request.getApprovalLevels() : 1)
                .requiresHrApproval(request.getRequiresHrApproval() != null ? request.getRequiresHrApproval() : false)
                .minNoticeDays(request.getMinNoticeDays() != null ? request.getMinNoticeDays() : 0)
                .maxConsecutiveDays(request.getMaxConsecutiveDays())
                .maxPerYear(request.getMaxPerYear())
                .allowNegativeBalance(request.getAllowNegativeBalance() != null ? request.getAllowNegativeBalance() : false)
                .build();
        return leaveTypeRepository.save(leaveType);
    }

    @Transactional(readOnly = true)
    public List<HrLeaveType> listLeaveTypes(Long orgId) {
        return leaveTypeRepository.findByOrganizationIdAndIsActiveTrue(orgId);
    }

    private LeaveRequestResponse toResponse(HrLeaveRequest lr) {
        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee().getId())
                .employeeName(lr.getEmployee().getFirstName() + " " + (lr.getEmployee().getLastName() != null ? lr.getEmployee().getLastName() : ""))
                .leaveTypeId(lr.getLeaveType().getId())
                .leaveTypeName(lr.getLeaveType().getLeaveTypeName())
                .fromDate(lr.getFromDate())
                .toDate(lr.getToDate())
                .totalDays(lr.getTotalDays())
                .reason(lr.getReason())
                .halfDay(lr.getIsHalfDay())
                .leaveSession(lr.getLeaveSession())
                .approvalStatus(lr.getApprovalStatus())
                .approvalLevel(lr.getApprovalLevel())
                .l1Remarks(lr.getL1Remarks())
                .l1ApprovedAt(lr.getL1ApprovedAt())
                .approverRemarks(lr.getApproverRemarks())
                .approvedAt(lr.getApprovedAt())
                .createdAt(lr.getCreatedAt())
                .build();
    }
}
