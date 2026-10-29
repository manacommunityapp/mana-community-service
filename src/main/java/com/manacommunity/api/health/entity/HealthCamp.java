package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.CampCategory;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "health_camp", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCamp extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampCategory category;

    @Column(name = "partner_provider_name", length = 255)
    private String partnerProviderName;

    @Column(nullable = false, length = 255)
    private String venue;

    @Column(name = "camp_date", nullable = false)
    private LocalDate campDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_slots", nullable = false)
    @Builder.Default
    private Integer maxSlots = 50;

    @Column(name = "registered_slots", nullable = false)
    @Builder.Default
    private Integer registeredSlots = 0;

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = true;

    @Column(name = "fee_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String description;
}
