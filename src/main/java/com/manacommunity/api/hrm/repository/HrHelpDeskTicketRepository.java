package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrHelpDeskTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrHelpDeskTicketRepository extends JpaRepository<HrHelpDeskTicket, Long> {
    Page<HrHelpDeskTicket> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<HrHelpDeskTicket> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
    Page<HrHelpDeskTicket> findByAssignedToIdAndStatus(Long assignedTo, String status, Pageable pageable);
    long countByOrganizationIdAndStatus(Long orgId, String status);
}
