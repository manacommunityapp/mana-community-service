package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_travel_requests")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrTravelRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private HrEmployee employee;
    @Column(name = "destination", nullable = false, length = 300) private String destination;
    @Column(name = "purpose", nullable = false, length = 500) private String purpose;
    @Column(name = "travel_from", nullable = false) private LocalDate travelFrom;
    @Column(name = "travel_to", nullable = false) private LocalDate travelTo;
    @Column(name = "mode_of_travel", length = 50) private String modeOfTravel;
    @Column(name = "estimated_cost") private Double estimatedCost;
    @Column(name = "advance_requested") @Builder.Default private Double advanceRequested = 0.0;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "PENDING";
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "rejection_reason", length = 500) private String rejectionReason;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
