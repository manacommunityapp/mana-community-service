package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_training_enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "training_program_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrTrainingEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "training_program_id", nullable = false)
    private HrTrainingProgram trainingProgram;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "ENROLLED";
    @Column(name = "enrolled_date") private LocalDate enrolledDate;
    @Column(name = "completion_date") private LocalDate completionDate;
    @Column(name = "score") private Integer score;
    @Column(name = "feedback", length = 500) private String feedback;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
