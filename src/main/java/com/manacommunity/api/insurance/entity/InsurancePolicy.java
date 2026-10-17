package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_policy")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InsurancePolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "community_id")
    private Long communityId;
    @Column(name = "partner_id")
    private Long partnerId;
    @Column(name = "insurer_name", nullable = false, length = 200)
    private String insurerName;
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsuranceEnums.Category category;
    @Column(name = "policy_number_last4", length = 4)
    private String policyNumberLast4;
    /** Encrypted at rest. Never serialised to clients; never logged. */
    @Column(name = "policy_number_enc", columnDefinition = "TEXT")
    private String policyNumberEnc;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private InsuranceEnums.PolicySource dataSource;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "sum_insured")
    private BigDecimal sumInsured;
    @Column(name = "premium_amount")
    private BigDecimal premiumAmount;
    @Column(name = "premium_frequency", length = 20)
    private String premiumFrequency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InsuranceEnums.PolicyStatus status;
    @Column(name = "nominee_name", length = 120)
    private String nomineeName;
    @Column(name = "insurer_contact", length = 300)
    private String insurerContact;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
