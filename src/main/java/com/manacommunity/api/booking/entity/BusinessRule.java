package com.manacommunity.api.booking.entity;

import com.manacommunity.api.booking.entity.enums.RuleType;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_rule", indexes = {
        @Index(name = "idx_business_rule_resource", columnList = "resource_id"),
        @Index(name = "idx_business_rule_category", columnList = "category_id"),
        @Index(name = "idx_business_rule_community", columnList = "community_id"),
        @Index(name = "idx_business_rule_type", columnList = "rule_type"),
        @Index(name = "idx_business_rule_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ResourceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private RuleType ruleType;

    @Column(name = "rule_key", length = 100)
    private String ruleKey;

    @Column(name = "rule_value", length = 500)
    private String ruleValue;

    @Column(name = "rule_operator", length = 20)
    private String ruleOperator;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column
    private Integer priority;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
