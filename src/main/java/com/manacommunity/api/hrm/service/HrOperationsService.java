package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import jakarta.persistence.EntityNotFoundException;
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
public class HrOperationsService {

    private final HrAssetRepository assetRepository;
    private final HrAssetAssignmentRepository assignmentRepository;
    private final HrTaskRepository taskRepository;
    private final HrAnnouncementRepository announcementRepository;
    private final HrHelpDeskTicketRepository ticketRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrAsset createAsset(Long orgId, HrAsset asset) {
        asset.setOrganizationId(orgId);
        return assetRepository.save(asset);
    }

    @Transactional
    public HrAssetAssignment assignAsset(Long orgId, Long assetId, Long employeeId, Long assignedBy, String condition) {
        HrAsset asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new EntityNotFoundException("Asset not found"));
        if (!"AVAILABLE".equals(asset.getStatus())) throw new IllegalStateException("Asset is not available for assignment");
        HrEmployee emp = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        asset.setStatus("ASSIGNED");
        assetRepository.save(asset);
        return assignmentRepository.save(HrAssetAssignment.builder()
            .organizationId(orgId).asset(asset).employee(emp)
            .assignedDate(LocalDate.now()).conditionAtAssignment(condition)
            .assignedBy(assignedBy).build());
    }

    @Transactional
    public HrAssetAssignment returnAsset(Long orgId, Long assignmentId, String conditionAtReturn) {
        HrAssetAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        assignment.setReturnDate(LocalDate.now());
        assignment.setConditionAtReturn(conditionAtReturn);
        assignment.setIsActive(false);
        assignment.getAsset().setStatus("AVAILABLE");
        assetRepository.save(assignment.getAsset());
        return assignmentRepository.save(assignment);
    }

    public Page<HrAsset> getAssets(Long orgId, String status, Pageable pageable) {
        if (status != null) return assetRepository.findByOrganizationIdAndStatus(orgId, status, pageable);
        return assetRepository.findByOrganizationId(orgId, pageable);
    }

    public List<HrAssetAssignment> getEmployeeAssets(Long employeeId) {
        return assignmentRepository.findByEmployeeIdAndIsActiveTrue(employeeId);
    }

    @Transactional
    public HrTask createTask(Long orgId, Long assignedToId, Long assignedById, HrTask task) {
        HrEmployee assignedTo = employeeRepository.findById(assignedToId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        task.setOrganizationId(orgId);
        task.setAssignedTo(assignedTo);
        if (assignedById != null) {
            employeeRepository.findById(assignedById).ifPresent(task::setAssignedBy);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public HrTask updateTaskStatus(Long taskId, String status) {
        HrTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        task.setStatus(status);
        if ("DONE".equals(status)) task.setCompletedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    public List<HrTask> getMyTasks(Long employeeId) {
        return taskRepository.findByAssignedToId(employeeId);
    }

    @Transactional
    public HrAnnouncement createAnnouncement(Long orgId, HrAnnouncement announcement, Long createdBy) {
        announcement.setOrganizationId(orgId);
        announcement.setCreatedBy(createdBy);
        if (announcement.getPublishedAt() == null) announcement.setPublishedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public List<HrAnnouncement> getActiveAnnouncements(Long orgId) {
        return announcementRepository.findActiveByOrganizationId(orgId);
    }

    @Transactional
    public HrHelpDeskTicket createTicket(Long orgId, Long employeeId, HrHelpDeskTicket ticket) {
        HrEmployee emp = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        ticket.setOrganizationId(orgId);
        ticket.setEmployee(emp);
        LocalDateTime now = LocalDateTime.now();
        long count = ticketRepository.countByOrganizationIdAndStatus(orgId, "OPEN")
            + ticketRepository.countByOrganizationIdAndStatus(orgId, "IN_PROGRESS") + 1;
        ticket.setTicketNumber(String.format("HD-%d%02d-%04d", now.getYear() % 100, now.getMonthValue(), count));
        return ticketRepository.save(ticket);
    }

    @Transactional
    public HrHelpDeskTicket assignTicket(Long orgId, Long ticketId, Long assignedToEmployeeId) {
        HrHelpDeskTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        HrEmployee assignedTo = employeeRepository.findById(assignedToEmployeeId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        ticket.setAssignedTo(assignedTo);
        ticket.setStatus("IN_PROGRESS");
        return ticketRepository.save(ticket);
    }

    @Transactional
    public HrHelpDeskTicket resolveTicket(Long orgId, Long ticketId, String resolution) {
        HrHelpDeskTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
        ticket.setStatus("RESOLVED");
        ticket.setResolution(resolution);
        ticket.setResolvedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Page<HrHelpDeskTicket> getMyTickets(Long employeeId, Pageable pageable) {
        return ticketRepository.findByEmployeeId(employeeId, pageable);
    }

    public Page<HrHelpDeskTicket> getOpenTickets(Long orgId, Pageable pageable) {
        return ticketRepository.findByOrganizationIdAndStatus(orgId, "OPEN", pageable);
    }
}
