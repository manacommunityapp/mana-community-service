package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_interviews")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrInterview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "applicant_id", nullable = false)
    private HrApplicant applicant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "interviewer_employee_id")
    private HrEmployee interviewer;
    @Column(name = "interview_type", length = 50) @Builder.Default private String interviewType = "VIDEO";
    @Column(name = "scheduled_at") private LocalDateTime scheduledAt;
    @Column(name = "duration_minutes") @Builder.Default private Integer durationMinutes = 60;
    @Column(name = "location", length = 200) private String location;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "SCHEDULED";
    @Column(name = "feedback", columnDefinition = "TEXT") private String feedback;
    @Column(name = "rating") private Integer rating;
    @Column(name = "outcome", length = 20) private String outcome;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
