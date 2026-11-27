package com.manacommunity.api.platform.dpdp;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity(name = "PlatformDataRightsRequest")
@Table(name = "dpdp_data_rights_requests")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DataRightsRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id")
    private Long residentId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "request_type", length = 50)
    private String requestType;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_at")
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
