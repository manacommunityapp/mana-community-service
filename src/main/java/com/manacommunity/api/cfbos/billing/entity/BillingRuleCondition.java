package com.manacommunity.api.cfbos.billing.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_billing_rule_condition")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRuleCondition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_rule_id", nullable = false)
    @JsonIgnore
    private BillingRule billingRule;
    @Column(nullable = false, length = 50) private String fieldName;
    @Column(nullable = false, length = 20) private String operator;
    @Column(nullable = false, length = 255) private String fieldValue;
    @Builder.Default private Integer logicalGroup = 0;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
