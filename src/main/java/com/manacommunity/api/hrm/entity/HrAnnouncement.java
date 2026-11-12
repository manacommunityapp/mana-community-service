package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_announcements")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrAnnouncement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "title", nullable = false, length = 300) private String title;
    @Column(name = "body", nullable = false, columnDefinition = "TEXT") private String body;
    @Column(name = "target_type", nullable = false, length = 20) @Builder.Default private String targetType = "ALL";
    @Column(name = "target_department_id") private Long targetDepartmentId;
    @Column(name = "target_employee_id") private Long targetEmployeeId;
    @Column(name = "is_pinned", nullable = false) @Builder.Default private Boolean isPinned = false;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
