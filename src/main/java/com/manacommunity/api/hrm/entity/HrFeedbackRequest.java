package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_feedback_requests")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrFeedbackRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subject_employee_id", nullable = false)
    private HrEmployee subjectEmployee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appraisal_cycle_id")
    private HrAppraisalCycle appraisalCycle;
    @Column(name = "deadline") private LocalDate deadline;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "OPEN";
    @Column(name = "is_anonymous", nullable = false) @Builder.Default private Boolean isAnonymous = true;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
