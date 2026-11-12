package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrLeaveRequest;
import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrLeaveRequestRepository extends JpaRepository<HrLeaveRequest, Long> {

    Page<HrLeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<HrLeaveRequest> findByOrganizationIdAndApprovalStatus(Long organizationId, LeaveApprovalStatus status, Pageable pageable);

    Optional<HrLeaveRequest> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("SELECT lr FROM HrLeaveRequest lr WHERE lr.employee.id = :employeeId " +
            "AND lr.approvalStatus IN ('PENDING', 'APPROVED') " +
            "AND lr.fromDate <= :toDate AND lr.toDate >= :fromDate")
    List<HrLeaveRequest> findOverlapping(Long employeeId, LocalDate fromDate, LocalDate toDate);

    long countByOrganizationIdAndApprovalStatus(Long organizationId, LeaveApprovalStatus status);
}
