package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_asset_assignments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrAssetAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "asset_id", nullable = false)
    private HrAsset asset;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "assigned_date", nullable = false) private LocalDate assignedDate;
    @Column(name = "return_date") private LocalDate returnDate;
    @Column(name = "condition_at_assignment", length = 50) private String conditionAtAssignment;
    @Column(name = "condition_at_return", length = 50) private String conditionAtReturn;
    @Column(name = "notes", length = 500) private String notes;
    @Column(name = "assigned_by", nullable = false) private Long assignedBy;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
