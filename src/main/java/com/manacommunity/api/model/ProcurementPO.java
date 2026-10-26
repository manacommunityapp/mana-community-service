package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurement_purchase_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 30)
    private String poNumber;

    @Column(name = "rfq_id")
    private Long rfqId;

    @Column(name = "rfq_number", length = 30)
    private String rfqNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierProfile supplier;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private POStatus status = POStatus.SENT;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<POItem> items = new ArrayList<>();

    @Column(name = "sub_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "gst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDate deliveryDeadline;

    @Column(name = "delivery_address", nullable = false, length = 400)
    private String deliveryAddress;

    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

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
