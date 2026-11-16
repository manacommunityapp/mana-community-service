package com.manacommunity.api.mw.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.mw.dto.CompleteAssignmentRequest;
import com.manacommunity.api.mw.dto.CreateOperationalAssignmentRequest;
import com.manacommunity.api.mw.entity.MwOperationalAssignment;
import com.manacommunity.api.mw.repository.MwOperationalAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MwOperationalAssignmentService {

    private final MwOperationalAssignmentRepository assignmentRepo;

    public MwOperationalAssignment createAssignment(Long orgId, CreateOperationalAssignmentRequest req) {
        return assignmentRepo.save(MwOperationalAssignment.builder()
                .organizationId(orgId)
                .sourceModule(req.getSourceModule())
                .sourceRefId(req.getSourceRefId())
                .employeeId(req.getEmployeeId())
                .assignmentType(req.getAssignmentType())
                .notes(req.getNotes())
                .incentiveAmount(req.getIncentiveAmount())
                .build());
    }

    public MwOperationalAssignment startAssignment(Long orgId, Long assignmentId) {
        MwOperationalAssignment a = getAndVerify(orgId, assignmentId);
        a.setStatus("IN_PROGRESS");
        a.setStartedAt(LocalDateTime.now());
        return assignmentRepo.save(a);
    }

    public MwOperationalAssignment completeAssignment(Long orgId, Long assignmentId, CompleteAssignmentRequest req) {
        MwOperationalAssignment a = getAndVerify(orgId, assignmentId);
        a.setStatus("COMPLETED");
        a.setCompletedAt(LocalDateTime.now());
        if (req.getPerformanceScore() != null) a.setPerformanceScore(req.getPerformanceScore());
        if (req.getNotes() != null) a.setNotes(req.getNotes());
        return assignmentRepo.save(a);
    }

    public List<MwOperationalAssignment> getAssignments(Long orgId, String sourceModule, String status) {
        if (sourceModule != null) {
            return assignmentRepo.findByOrganizationIdAndSourceModuleOrderByAssignedAtDesc(orgId, sourceModule);
        }
        if (status != null) {
            return assignmentRepo.findByOrganizationIdAndStatusOrderByAssignedAtDesc(orgId, status);
        }
        return assignmentRepo.findByOrganizationIdOrderByAssignedAtDesc(orgId);
    }

    public List<MwOperationalAssignment> getEmployeeAssignments(Long orgId, Long employeeId) {
        return assignmentRepo.findByOrganizationIdAndEmployeeIdOrderByAssignedAtDesc(orgId, employeeId);
    }

    private MwOperationalAssignment getAndVerify(Long orgId, Long assignmentId) {
        MwOperationalAssignment a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (!a.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not in your org");
        }
        return a;
    }
}
