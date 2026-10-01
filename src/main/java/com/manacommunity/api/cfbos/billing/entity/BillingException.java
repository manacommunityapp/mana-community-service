package com.manacommunity.api.cfbos.billing.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_billing_exception")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingException {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long propertyId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_rule_id", nullable = false)
    private BillingRule billingRule;
    @Column(nullable = false, length = 20) private String exceptionType;
    @Column(precision = 18, scale = 2) private BigDecimal overrideAmount;
    @Column(precision = 18, scale = 4) private BigDecimal overrideRate;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Column(nullable = false) private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Long approvedBy;
    @Builder.Default private Boolean isActive = true;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
