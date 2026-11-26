package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_run_line_item", uniqueConstraints = {
    @UniqueConstraint(name = "uq_billing_run_flat",
                      columnNames = {"billing_run_id", "resident_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingRunLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_run_id", nullable = false)
    @ToString.Exclude
    private BillingRun billingRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    @ToString.Exclude
    private AppUser resident;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(name = "maintenance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maintenanceAmount;

    @Column(name = "sinking_fund", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sinkingFund = BigDecimal.ZERO;

    @Column(name = "water_charges", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal waterCharges = BigDecimal.ZERO;

    @Column(name = "parking_charges", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal parkingCharges = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "cgst", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(name = "sgst", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "GENERATED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @ToString.Exclude
    private Invoice invoice;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
