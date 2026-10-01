package com.manacommunity.api.cfbos.unit.approval;

import com.manacommunity.api.cfbos.approval.dto.*;
import com.manacommunity.api.cfbos.approval.entity.*;
import com.manacommunity.api.cfbos.approval.repository.*;
import com.manacommunity.api.cfbos.approval.service.ApprovalWorkflowService;
import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceTest {

    @Mock private ApprovalWorkflowRepository workflowRepository;
    @Mock private ApprovalRequestRepository requestRepository;
    @Mock private ApprovalActionRepository actionRepository;

    private ApprovalWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalWorkflowService(workflowRepository, requestRepository, actionRepository);
    }

    @Test
    @DisplayName("Submit request without workflow auto-approves")
    void submitWithoutWorkflowAutoApproves() {
        when(workflowRepository.findByEntityTypeAndIsActiveTrue("EXPENSE")).thenReturn(Optional.empty());
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(i -> {
            ApprovalRequest r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        SubmitApprovalRequest req = SubmitApprovalRequest.builder()
                .entityType("EXPENSE")
                .entityId(100L)
                .amount(new BigDecimal("5000.00"))
                .remarks("Test expense")
                .submittedBy(10L)
                .build();

        ApprovalResponse res = service.submitRequest(req);
        assertThat(res.getStatus()).isEqualTo(ApprovalState.APPROVED);
        assertThat(res.getEntityId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Submit request with multi-step workflow creates pending request")
    void submitWithWorkflowCreatesPending() {
        ApprovalStep step1 = ApprovalStep.builder().stepOrder(1).approverRole("TREASURER").isAutoApprove(false).build();
        ApprovalWorkflow wf = ApprovalWorkflow.builder().id(1L).name("Expense WF").steps(List.of(step1)).build();

        when(workflowRepository.findByEntityTypeAndIsActiveTrue("EXPENSE")).thenReturn(Optional.of(wf));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(i -> {
            ApprovalRequest r = i.getArgument(0);
            r.setId(2L);
            return r;
        });

        SubmitApprovalRequest req = SubmitApprovalRequest.builder()
                .entityType("EXPENSE")
                .entityId(101L)
                .amount(new BigDecimal("50000.00"))
                .remarks("Capital expense")
                .submittedBy(10L)
                .build();

        ApprovalResponse res = service.submitRequest(req);
        assertThat(res.getStatus()).isEqualTo(ApprovalState.PENDING);
        assertThat(res.getCurrentStep()).isEqualTo(1);
    }
}
