package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A Mana assistance ticket. It is NOT an insurer claim and carries no insurer decision state. */
@Entity
@Table(name = "insurance_claims_assistance_case")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimsAssistanceCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "case_ref", nullable = false, unique = true, length = 24)
    private String caseRef;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "community_id")
    private Long communityId;
    @Column(name = "policy_id")
    private Long policyId;
    @Column(name = "claim_type", nullable = false, length = 30)
    private String claimType;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InsuranceEnums.CaseStatus status = InsuranceEnums.CaseStatus.OPEN;
    @Column(name = "assigned_team", length = 60)
    private String assignedTeam;
    @Column(name = "insurer_claim_reference", length = 80)
    private String insurerClaimReference;
    @Column(name = "insurer_claim_reference_source", length = 20)
    private String insurerClaimReferenceSource;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
