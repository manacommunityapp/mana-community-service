package com.manacommunity.api.forensics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_merkle_anchors", schema = "forensics")
@Getter
@Setter
public class ExternalMerkleAnchorEntity {

    @Id
    @Column(name = "anchor_id", nullable = false)
    private UUID anchorId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "seq_from", nullable = false)
    private Long seqFrom;

    @Column(name = "seq_to", nullable = false)
    private Long seqTo;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount;

    @Column(name = "head_hash", nullable = false, length = 64)
    private String headHash;

    @Column(name = "merkle_root", nullable = false, length = 64)
    private String merkleRoot;

    @Column(name = "ecdsa_signature", nullable = false, length = 256)
    private String ecdsaSignature;

    @Column(name = "worm_bucket_uri", nullable = false, length = 240)
    private String wormBucketUri;

    @Column(name = "object_lock_mode", nullable = false, length = 30)
    private String objectLockMode = "COMPLIANCE";

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Column(name = "anchored_at", nullable = false)
    private Instant anchoredAt;
}
