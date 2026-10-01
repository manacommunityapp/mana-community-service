package com.manacommunity.api.cfbos.approval.service;

import com.manacommunity.api.cfbos.approval.dto.*;
import com.manacommunity.api.cfbos.approval.entity.*;
import com.manacommunity.api.cfbos.approval.repository.*;
import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;

    @Transactional
    public ApprovalResponse submitRequest(SubmitApprovalRequest request) {
        ApprovalWorkflow workflow = workflowRepository.findByEntityTypeAndIsActiveTrue(request.getEntityType())
                .orElse(null);

        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            ApprovalRequest autoApproved = ApprovalRequest.builder()
                    .workflow(workflow)
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .amount(request.getAmount())
                    .submittedBy(request.getSubmittedBy() != null ? request.getSubmittedBy() : 1L)
                    .submittedAt(LocalDateTime.now())
                    .status(ApprovalState.APPROVED)
                    .remarks(request.getRemarks() != null ? request.getRemarks() + " (Auto-approved: no workflow defined)" : "Auto-approved")
                    .currentStep(1)
                    .build();
            autoApproved = requestRepository.save(autoApproved);
            return toResponse(autoApproved);
        }

        ApprovalStep firstStep = workflow.getSteps().get(0);
        boolean autoApprove = false;
        if (Boolean.TRUE.equals(firstStep.getIsAutoApprove()) ||
                (firstStep.getAutoApproveBelow() != null && request.getAmount() != null &&
                        request.getAmount().compareTo(firstStep.getAutoApproveBelow()) < 0)) {
            autoApprove = true;
        }

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .workflow(workflow)
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .amount(request.getAmount())
                .submittedBy(request.getSubmittedBy() != null ? request.getSubmittedBy() : 1L)
                .submittedAt(LocalDateTime.now())
                .status(autoApprove ? ApprovalState.APPROVED : ApprovalState.PENDING)
                .remarks(request.getRemarks())
                .currentStep(1)
                .actions(new ArrayList<>())
                .build();

        approvalRequest = requestRepository.save(approvalRequest);

        if (autoApprove) {
            ApprovalAction action = ApprovalAction.builder()
                    .request(approvalRequest)
                    .stepOrder(1)
                    .action(ApprovalState.APPROVED)
                    .actorId(0L)
                    .actedAt(LocalDateTime.now())
                    .comments("System auto-approved below threshold")
                    .build();
            actionRepository.save(action);
            approvalRequest.getActions().add(action);
        }

        return toResponse(approvalRequest);
    }

    @Transactional
    public ApprovalResponse processAction(ApprovalActionRequest actionReq) {
        ApprovalRequest request = requestRepository.findById(actionReq.getRequestId())
                .orElseThrow(() -> new CfbosResourceNotFoundException("ApprovalRequest", actionReq.getRequestId()));

        if (request.getStatus() != ApprovalState.PENDING) {
            throw new CfbosException("Approval request is not in PENDING state: " + request.getStatus());
        }

        ApprovalAction action = ApprovalAction.builder()
                .request(request)
                .stepOrder(request.getCurrentStep())
                .action(actionReq.getAction())
                .actorId(actionReq.getActorId())
                .actedAt(LocalDateTime.now())
                .comments(actionReq.getComments())
                .build();

        actionRepository.save(action);
        request.getActions().add(action);

        if (actionReq.getAction() == ApprovalState.REJECTED) {
            request.setStatus(ApprovalState.REJECTED);
        } else if (actionReq.getAction() == ApprovalState.APPROVED) {
            List<ApprovalStep> steps = request.getWorkflow() != null ? request.getWorkflow().getSteps() : List.of();
            if (request.getCurrentStep() >= steps.size()) {
                request.setStatus(ApprovalState.APPROVED);
            } else {
                request.setCurrentStep(request.getCurrentStep() + 1);
            }
        }

        request = requestRepository.save(request);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public ApprovalResponse getRequestById(Long id) {
        ApprovalRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("ApprovalRequest", id));
        return toResponse(req);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> getPendingRequests() {
        return requestRepository.findByStatus(ApprovalState.PENDING)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ApprovalResponse toResponse(ApprovalRequest entity) {
        return ApprovalResponse.builder()
                .id(entity.getId())
                .workflowId(entity.getWorkflow() != null ? entity.getWorkflow().getId() : null)
                .workflowName(entity.getWorkflow() != null ? entity.getWorkflow().getName() : "Direct Approval")
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .currentStep(entity.getCurrentStep())
                .status(entity.getStatus())
                .submittedBy(entity.getSubmittedBy())
                .submittedAt(entity.getSubmittedAt())
                .amount(entity.getAmount())
                .remarks(entity.getRemarks())
                .actions(entity.getActions() != null ? entity.getActions().stream()
                        .map(a -> ApprovalResponse.ActionRecord.builder()
                                .id(a.getId())
                                .stepOrder(a.getStepOrder())
                                .action(a.getAction())
                                .actorId(a.getActorId())
                                .actedAt(a.getActedAt())
                                .comments(a.getComments())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .build();
    }
}
