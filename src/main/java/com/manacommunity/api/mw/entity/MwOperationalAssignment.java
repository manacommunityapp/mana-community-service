package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_operational_assignments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwOperationalAssignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    @Column(length = 50, nullable = false)
    private String sourceModule;

    private Long sourceRefId;

    private Long employeeId;

    @Column(length = 100)
    private String assignmentType;

    @Column(length = 30)
    private String status;

    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private BigDecimal incentiveAmount;
    private Integer performanceScore;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    void onCreate() {
        assignedAt = LocalDateTime.now();
        if (status == null) status = "ASSIGNED";
    }
}
