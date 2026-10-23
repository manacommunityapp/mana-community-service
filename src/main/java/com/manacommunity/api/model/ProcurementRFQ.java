package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurement_rfqs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementRFQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_number", nullable = false, unique = true, length = 30)
    private String rfqNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RFQStatus status = RFQStatus.DRAFT;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<RFQItem> items = new ArrayList<>();

    @Column(name = "delivery_location", nullable = false, length = 300)
    private String deliveryLocation;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDate deliveryDeadline;

    @Column(name = "quotation_deadline", nullable = false)
    private LocalDate quotationDeadline;

    @Column(name = "estimated_value", precision = 14, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "approved_budget", precision = 14, scale = 2)
    private BigDecimal approvedBudget;

    @Column(name = "invited_supplier_ids", length = 500)
    private String invitedSupplierIds;

    @Column(name = "quotes_received")
    @Builder.Default
    private Integer quotesReceived = 0;

    @Column(length = 800)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
