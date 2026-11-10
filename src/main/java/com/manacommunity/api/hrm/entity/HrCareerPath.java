package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_career_paths")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrCareerPath {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "current_role", length = 200) private String currentRole;
    @Column(name = "target_role", length = 200) private String targetRole;
    @Column(name = "target_date") private LocalDate targetDate;
    @Column(name = "development_activities", columnDefinition = "TEXT") private String developmentActivities;
    @Column(name = "skills_to_develop", columnDefinition = "TEXT") private String skillsToDevelop;
    @Column(name = "mentor_employee_id") private Long mentorEmployeeId;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
