package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_okrs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrOkr {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id")
    private HrEmployee employee;
    @Column(name = "objective", nullable = false, length = 500) private String objective;
    @Column(name = "quarter", length = 10) private String quarter;
    @Column(name = "year") private Integer year;
    @Column(name = "overall_progress") @Builder.Default private Integer overallProgress = 0;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
