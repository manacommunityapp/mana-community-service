package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_salary_components")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrSalaryComponent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "code", nullable = false, length = 50) private String code;
    @Column(name = "component_type", nullable = false, length = 20) private String componentType;
    @Column(name = "calculation_type", nullable = false, length = 30) private String calculationType;
    @Column(name = "default_value") private Double defaultValue;
    @Column(name = "is_taxable", nullable = false) @Builder.Default private Boolean isTaxable = false;
    @Column(name = "is_statutory", nullable = false) @Builder.Default private Boolean isStatutory = false;
    @Column(name = "statutory_code", length = 20) private String statutoryCode;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
    @Column(name = "sequence_order") @Builder.Default private Integer sequenceOrder = 0;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
