package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.MedicineOrderStatus;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "health_medicine_order", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMedicineOrder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID orderUuid = UUID.randomUUID();

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "delivery_slot", length = 80)
    private String deliverySlot;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "total", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MedicineOrderStatus status = MedicineOrderStatus.PLACED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<HealthMedicineOrderItem> items;
}
