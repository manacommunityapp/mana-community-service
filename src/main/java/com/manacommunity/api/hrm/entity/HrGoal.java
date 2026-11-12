package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_goals")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appraisal_cycle_id")
    private HrAppraisalCycle appraisalCycle;
    @Column(name = "title", nullable = false, length = 300) private String title;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "is_kra", nullable = false) @Builder.Default private Boolean isKra = false;
    @Column(name = "weightage") @Builder.Default private Integer weightage = 0;
    @Column(name = "target_value", length = 200) private String targetValue;
    @Column(name = "achievement_percent") @Builder.Default private Integer achievementPercent = 0;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
