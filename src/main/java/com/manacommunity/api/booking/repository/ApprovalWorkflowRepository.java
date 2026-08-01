package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

    List<ApprovalWorkflow> findByResourceIdAndIsActiveTrueOrderByStepOrderAsc(Long resourceId);

    List<ApprovalWorkflow> findByCategoryIdAndIsActiveTrueOrderByStepOrderAsc(Long categoryId);
}
