package com.manacommunity.api.identity.portability.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "community_federation_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityFederationLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id_a", nullable = false)
    private Long communityIdA;

    @Column(name = "community_id_b", nullable = false)
    private Long communityIdB;

    @Column(name = "link_type", nullable = false, length = 30)
    @Builder.Default
    private String linkType = "IDENTITY_SHARE";

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
