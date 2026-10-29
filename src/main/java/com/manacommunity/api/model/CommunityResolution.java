package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_resolution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityResolution extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resolution_number", nullable = false)
    private String resolutionNumber;

    @Column(nullable = false)
    private String title;

    @Column(name = "meeting_reference")
    private String meetingReference;

    @Column(name = "passed_date")
    private LocalDate passedDate;

    @Column
    private String category;

    @Column(name = "approved_budget")
    private String approvedBudget;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PASSED"; // PASSED, IN_IMPLEMENTATION, COMPLETED

    @Column(name = "for_votes")
    @Builder.Default
    private Integer forVotes = 0;

    @Column(name = "against_votes")
    @Builder.Default
    private Integer againstVotes = 0;

    @Column(name = "abstained_votes")
    @Builder.Default
    private Integer abstainedVotes = 0;

    @Column(name = "presiding_officer")
    private String presidingOfficer;

    @Column(name = "legal_binding_statement", columnDefinition = "TEXT")
    private String legalBindingStatement;

    @Column(name = "document_url")
    private String documentUrl;

    @ElementCollection
    @CollectionTable(name = "community_resolution_provisions", joinColumns = @JoinColumn(name = "resolution_id"))
    @Column(name = "provision_text")
    @Builder.Default
    private List<String> keyProvisions = new ArrayList<>();
}
