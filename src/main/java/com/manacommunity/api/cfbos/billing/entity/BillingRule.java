package com.manacommunity.api.cfbos.billing.entity;
import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import com.manacommunity.api.cfbos.shared.entity.CfbosBaseFields;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfbos_billing_rule")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_category_id", nullable = false)
    private BillingCategory billingCategory;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_type_id", nullable = false)
    private ChargeType chargeType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_schedule_id", nullable = false)
    private BillingSchedule billingSchedule;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30) private CalculationMethod calculationMethod;
    @Column(precision = 18, scale = 2) private BigDecimal fixedAmount;
    @Column(precision = 18, scale = 4) private BigDecimal ratePerUnit;
    private Long formulaId;
    private Long slabConfigId;
    @Builder.Default private Boolean isTaxable = true;
    private Long taxRateId;
    private Long hsnSacCodeId;
    @Column(nullable = false) private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Builder.Default private Boolean isActive = true;
    @Builder.Default private Integer priority = 0;
    @OneToMany(mappedBy = "billingRule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<BillingRuleCondition> conditions = new ArrayList<>();
    @Embedded @Builder.Default private CfbosBaseFields baseFields = new CfbosBaseFields();
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
