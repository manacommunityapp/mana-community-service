package com.manacommunity.api.cfbos.approval.repository;

import com.manacommunity.api.cfbos.approval.entity.CfbosApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CfbosApprovalWorkflowRepository extends JpaRepository<CfbosApprovalWorkflow, Long> {
    Optional<CfbosApprovalWorkflow> findByEntityTypeAndIsActiveTrue(String entityType);
    Optional<CfbosApprovalWorkflow> findByName(String name);
}
