package com.manacommunity.api.booking.entity;

import com.manacommunity.api.booking.entity.enums.WorkflowStepType;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_workflow", indexes = {
        @Index(name = "idx_approval_workflow_resource", columnList = "resource_id"),
        @Index(name = "idx_approval_workflow_category", columnList = "category_id"),
        @Index(name = "idx_approval_workflow_community", columnList = "community_id"),
        @Index(name = "idx_approval_workflow_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ResourceCategory category;

    @Column(name = "workflow_name", nullable = false, length = 100)
    private String workflowName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private WorkflowStepType stepType;

    @Column(name = "step_name", length = 100)
    private String stepName;

    @Column(name = "is_required")
    @Builder.Default
    private boolean isRequired = true;

    @Column(name = "approver_role", length = 50)
    private String approverRole;

    @Column(name = "timeout_hours")
    private Integer timeoutHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
