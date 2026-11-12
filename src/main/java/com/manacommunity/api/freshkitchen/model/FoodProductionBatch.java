package com.manacommunity.api.freshkitchen.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_production_batch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FoodProductionBatch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false, unique = true, length = 80)
    private String batchNumber;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "vendor_id", nullable = false, length = 64)
    private String vendorId;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Column(name = "batch_quantity", nullable = false)
    @Builder.Default
    private Integer batchQuantity = 0;

    @Column(name = "storage_temp_celsius", precision = 4, scale = 1)
    private BigDecimal storageTempCelsius;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_status", nullable = false, length = 40)
    @Builder.Default
    private BatchStatus batchStatus = BatchStatus.PLANNED;

    @Column(name = "expiry_date_time", nullable = false)
    private LocalDateTime expiryDateTime;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by", length = 100)
    private String releasedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
