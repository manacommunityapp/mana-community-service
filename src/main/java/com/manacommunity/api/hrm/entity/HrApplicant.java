package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_applicants")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrApplicant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "job_posting_id", nullable = false)
    private HrJobPosting jobPosting;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", length = 100) private String lastName;
    @Column(name = "email", nullable = false, length = 200) private String email;
    @Column(name = "phone", length = 20) private String phone;
    @Column(name = "resume_url", length = 500) private String resumeUrl;
    @Column(name = "cover_letter", columnDefinition = "TEXT") private String coverLetter;
    @Column(name = "current_employer", length = 200) private String currentEmployer;
    @Column(name = "experience_years") private Integer experienceYears;
    @Column(name = "expected_salary") private Double expectedSalary;
    @Column(name = "status", nullable = false, length = 30) @Builder.Default private String status = "APPLIED";
    @Column(name = "source", length = 100) private String source;
    @Column(name = "notes", columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
