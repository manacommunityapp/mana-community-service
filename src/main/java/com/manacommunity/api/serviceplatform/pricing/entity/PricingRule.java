package com.manacommunity.api.serviceplatform.pricing.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sp_pricing_rule")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PricingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency")
    private ServiceUrgency urgency;

    @Column(name = "multiplier", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal multiplier = BigDecimal.ONE;

    @Column(name = "flat_surcharge", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal flatSurcharge = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
