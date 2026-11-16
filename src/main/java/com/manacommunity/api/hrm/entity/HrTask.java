package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_tasks")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_employee_id", nullable = false)
    private HrEmployee assignedTo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_by_employee_id")
    private HrEmployee assignedBy;
    @Column(name = "title", nullable = false, length = 300) private String title;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "priority", nullable = false, length = 10) @Builder.Default private String priority = "MEDIUM";
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "OPEN";
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
