package com.manacommunity.api.fresh.recall.model;

import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fresh_product_recall", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshProductRecall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recall_reference", unique = true, nullable = false, length = 50)
    private String recallReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private FoodProductionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false)
    private VendorProduct vendorProduct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RecallSeverity severity = RecallSeverity.MAJOR;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "detailed_root_cause", columnDefinition = "TEXT")
    private String detailedRootCause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    private AppUser initiatedByUser;

    @CreationTimestamp
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RecallStatus status = RecallStatus.INITIATED;

    @Column(name = "affected_orders_count", nullable = false)
    @Builder.Default
    private Integer affectedOrdersCount = 0;

    @Column(name = "affected_customers_count", nullable = false)
    @Builder.Default
    private Integer affectedCustomersCount = 0;

    @Column(name = "total_units_recalled", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal totalUnitsRecalled = BigDecimal.ZERO;

    @Column(name = "total_refund_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "recall", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FreshProductRecallOrder> affectedOrders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
