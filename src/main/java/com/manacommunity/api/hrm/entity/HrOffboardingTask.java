package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_offboarding_tasks")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrOffboardingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;

    @Column(name = "task_title", nullable = false, length = 200)
    private String taskTitle;

    @Column(name = "task_description", length = 1000)
    private String taskDescription;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
