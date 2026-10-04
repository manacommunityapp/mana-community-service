package com.manacommunity.api.iot.metering;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_billing_cycle_summaries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"meter_id", "billing_month"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterBillingCycleSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private SmartMeter meter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "unit_number", nullable = false, length = 50)
    private String unitNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 50)
    private MeterType meterType;

    @Column(name = "billing_month", nullable = false, length = 10)
    private String billingMonth;

    @Column(name = "start_reading", nullable = false, precision = 14, scale = 4)
    private BigDecimal startReading;

    @Column(name = "end_reading", nullable = false, precision = 14, scale = 4)
    private BigDecimal endReading;

    @Column(name = "total_units_consumed", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalUnitsConsumed;

    @Column(name = "slab_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal slabAmount;

    @Column(name = "cfbos_invoice_id")
    private Long cfbosInvoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", nullable = false, length = 50)
    @Builder.Default
    private MeterBillingStatus billingStatus = MeterBillingStatus.PENDING_SYNC;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
