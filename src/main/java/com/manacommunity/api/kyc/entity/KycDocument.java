package com.manacommunity.api.kyc.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kyc_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This entity is serialised straight back to the client (see KycSummaryResponse),
    // so the lazy owner/community associations are kept out of the JSON: a proxy would
    // fail to serialise outside the session, and the full AppUser graph is PII that has
    // no business in a KYC response.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    /**
     * HMAC-SHA256(server pepper, client SHA-256 digest) of the government ID number.
     * Never the raw number, and never the client digest on its own — see
     * {@code KycIdNumberProtector} for why the client digest alone is not enough.
     * 100 chars matches kyc_verification_requests.id_number_hash.
     */
    @Column(name = "id_number_hash", length = 100)
    private String idNumberHash;

    /** Last 1-4 characters of the ID number. Display only: the UI renders the rest masked. */
    @Column(name = "id_number_last4", length = 4)
    private String idNumberLast4;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING_REVIEW";

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "front_image_url", columnDefinition = "TEXT")
    private String frontImageUrl;

    @Column(name = "back_image_url", columnDefinition = "TEXT")
    private String backImageUrl;
}
