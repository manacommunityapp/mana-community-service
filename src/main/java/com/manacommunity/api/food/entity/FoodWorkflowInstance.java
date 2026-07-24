package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_workflow_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodWorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    private FoodWorkflowDefinition definition;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "current_step")
    private Integer currentStep;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WorkflowStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum WorkflowStatus {
        IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }
}
