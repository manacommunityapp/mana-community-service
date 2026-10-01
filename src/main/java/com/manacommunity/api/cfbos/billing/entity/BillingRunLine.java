package com.manacommunity.api.cfbos.billing.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_billing_run_line")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRunLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_run_id", nullable = false)
    @JsonIgnore
    private BillingRun billingRun;
    @Column(nullable = false) private Long propertyId;
    @Column(nullable = false) private Long residentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_rule_id", nullable = false)
    private BillingRule billingRule;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_type_id", nullable = false)
    private ChargeType chargeType;
    private String description;
    @Builder.Default private BigDecimal quantity = BigDecimal.ONE;
    private BigDecimal rate;
    private BigDecimal amount;
    @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount;
    @Column(columnDefinition = "TEXT") private String calculationDetails;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
