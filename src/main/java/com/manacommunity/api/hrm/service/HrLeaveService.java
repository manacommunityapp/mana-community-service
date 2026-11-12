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

@Service
@RequiredArgsConstructor
public class HrLeaveService {

    private final HrLeaveRequestRepository leaveRequestRepository;
    private final HrLeaveBalanceRepository leaveBalanceRepository;
    private final HrLeaveTypeRepository leaveTypeRepository;

    @Transactional
    public LeaveRequestResponse applyLeave(HrEmployee employee, ApplyLeaveRequest request) {
        HrLeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));

        if (!leaveType.getOrganization().getId().equals(employee.getOrganization().getId())) {
            throw new IllegalArgumentException("Leave type does not belong to this organization");
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
        if (balance.getRemainingBalance() < request.getTotalDays()) {
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
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse approveLeave(Long id, Long orgId, Long approvedBy, LeaveActionRequest request) {
        HrLeaveRequest leaveRequest = leaveRequestRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        if (leaveRequest.getApprovalStatus() != LeaveApprovalStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in PENDING status");
        }

        leaveRequest.setApprovalStatus(LeaveApprovalStatus.APPROVED);
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApproverRemarks(request.getApproverRemarks());
        leaveRequest.setApprovedAt(LocalDateTime.now());
        HrLeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        int year = savedRequest.getFromDate().getYear();
        HrEmployee emp = savedRequest.getEmployee();
        HrLeaveType lt = savedRequest.getLeaveType();
        HrLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(emp.getId(), lt.getId(), year)
                .orElseGet(() -> HrLeaveBalance.builder()
                        .organizationId(orgId)
                        .employee(emp)
                        .leaveType(lt)
                        .year(year)
                        .totalBalance((double) lt.getDefaultBalance())
                        .usedBalance(0.0)
                        .remainingBalance((double) lt.getDefaultBalance())
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

        if (leaveRequest.getApprovalStatus() != LeaveApprovalStatus.PENDING) {
            throw new IllegalStateException("Leave request is not in PENDING status");
        }

        leaveRequest.setApprovalStatus(LeaveApprovalStatus.REJECTED);
        leaveRequest.setApprovedBy(rejectedBy);
        leaveRequest.setApproverRemarks(request.getApproverRemarks());
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        return toResponse(leaveRequest);
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
                .approvalStatus(lr.getApprovalStatus())
                .approverRemarks(lr.getApproverRemarks())
                .approvedAt(lr.getApprovedAt())
                .createdAt(lr.getCreatedAt())
                .build();
    }
}
