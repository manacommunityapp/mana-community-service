package com.manacommunity.api.procurement.repository;

import com.manacommunity.api.procurement.entity.ProcApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcApprovalWorkflowRepository extends JpaRepository<ProcApprovalWorkflow, Long> {

    Optional<ProcApprovalWorkflow> findByInvoiceId(Long invoiceId);

    List<ProcApprovalWorkflow> findByCommunityIdAndStatus(Long communityId, ProcApprovalWorkflow.WorkflowStatus status);

    List<ProcApprovalWorkflow> findByCommunityIdAndCurrentLevel(Long communityId, ProcApprovalWorkflow.ApprovalLevel level);

    List<ProcApprovalWorkflow> findByCommunityId(Long communityId);
}
