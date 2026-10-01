package com.manacommunity.api.cfbos.approval.repository;

import com.manacommunity.api.cfbos.approval.entity.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    Optional<ApprovalWorkflow> findByEntityTypeAndIsActiveTrue(String entityType);
    Optional<ApprovalWorkflow> findByName(String name);
}
