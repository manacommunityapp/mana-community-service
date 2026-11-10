package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_appraisal_forms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "appraisal_cycle_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrAppraisalForm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appraisal_cycle_id", nullable = false)
    private HrAppraisalCycle appraisalCycle;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_employee_id")
    private HrEmployee reviewer;
    @Column(name = "self_rating") private Integer selfRating;
    @Column(name = "self_comments", columnDefinition = "TEXT") private String selfComments;
    @Column(name = "manager_rating") private Integer managerRating;
    @Column(name = "manager_comments", columnDefinition = "TEXT") private String managerComments;
    @Column(name = "final_rating") private Integer finalRating;
    @Column(name = "final_comments", columnDefinition = "TEXT") private String finalComments;
    @Column(name = "status", nullable = false, length = 30) @Builder.Default private String status = "PENDING_SELF";
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
