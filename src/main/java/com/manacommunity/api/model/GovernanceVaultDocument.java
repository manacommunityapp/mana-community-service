package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "governance_vault_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceVaultDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String category = "MINUTES"; // BYLAWS, AUDIT, MINUTES, FORMS, LEGAL

    @Column(name = "file_size")
    private String fileSize;

    @Column(name = "file_format")
    private String fileFormat;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "is_confidential")
    @Builder.Default
    private Boolean isConfidential = false;

    @Column(name = "resolution_ref")
    private String resolutionRef;

    @Column(name = "digital_seal_verified")
    @Builder.Default
    private Boolean digitalSealVerified = false;
}
