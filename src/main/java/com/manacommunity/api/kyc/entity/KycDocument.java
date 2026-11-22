package com.manacommunity.api.kyc.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    /** HMAC-SHA256(server pepper, client SHA-256 of ID). See KycIdNumberProtector. Never raw number. */
    @Column(name = "id_number_hash", length = 100)
    private String idNumberHash;

    /** Last 1-4 chars of the government ID, for masked display only. */
    @Column(name = "id_number_last4", length = 4)
    private String idNumberLast4;

    /** @deprecated Legacy column; raw/encrypted number from old onboarding path. Never write from new code. */
    @Deprecated
    @Column(name = "document_number", length = 50)
    private String documentNumber;

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
