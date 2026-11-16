package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_proposal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityProposal extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "submitted_by_user_id", nullable = false)
    private Long submittedByUserId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String category = "AMENITIES"; // INFRASTRUCTURE, ENVIRONMENT, AMENITIES, SECURITY, FINANCE, RULES

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "PROPOSAL"; // PROPOSAL, GRIEVANCE

    @Column(name = "estimated_cost")
    private String estimatedCost;

    @Column(name = "support_threshold")
    @Builder.Default
    private Integer supportThreshold = 50;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SUBMITTED"; // SUBMITTED, UNDER_REVIEW, APPROVED_FOR_AGM, REJECTED, PASSED

    @Column(name = "committee_notes", columnDefinition = "TEXT")
    private String committeeNotes;

    @ElementCollection
    @CollectionTable(name = "community_proposal_pros", joinColumns = @JoinColumn(name = "proposal_id"))
    @Column(name = "pro_text")
    @Builder.Default
    private List<String> pros = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "community_proposal_cons", joinColumns = @JoinColumn(name = "proposal_id"))
    @Column(name = "con_text")
    @Builder.Default
    private List<String> cons = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProposalSupport> supports = new ArrayList<>();
}
