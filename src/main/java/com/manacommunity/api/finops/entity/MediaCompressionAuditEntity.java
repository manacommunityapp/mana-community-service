package com.manacommunity.api.finops.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_compression_audit", schema = "finops")
@Getter
@Setter
@NoArgsConstructor
public class MediaCompressionAuditEntity {

    @Id
    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "asset_class", nullable = false, length = 50)
    private String assetClass;

    @Column(name = "original_bytes", nullable = false)
    private long originalBytes;

    @Column(name = "compressed_bytes", nullable = false)
    private long compressedBytes;

    @Column(name = "format", nullable = false, length = 20)
    private String format;

    @Column(name = "storage_tier", nullable = false, length = 40)
    private String storageTier = "S3_STANDARD";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
