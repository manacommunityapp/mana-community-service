package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_job_postings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrJobPosting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "title", nullable = false, length = 200) private String title;
    @Column(name = "department_id") private Long departmentId;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "requirements", columnDefinition = "TEXT") private String requirements;
    @Column(name = "openings", nullable = false) @Builder.Default private Integer openings = 1;
    @Column(name = "location", length = 200) private String location;
    @Column(name = "employment_type", length = 50) private String employmentType;
    @Column(name = "min_salary") private Double minSalary;
    @Column(name = "max_salary") private Double maxSalary;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "DRAFT";
    @Column(name = "posted_date") private LocalDate postedDate;
    @Column(name = "closing_date") private LocalDate closingDate;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
