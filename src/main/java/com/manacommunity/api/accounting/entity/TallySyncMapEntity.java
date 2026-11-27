package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tally_sync_map", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class TallySyncMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "tally_guid", nullable = false)
    private UUID tallyGuid;

    @Column(name = "tally_voucher_number", nullable = false, length = 100)
    private String tallyVoucherNumber;

    @Column(name = "tally_alter_id", nullable = false)
    private long tallyAlterId = 0L;

    @Column(name = "cloud_entry_hash", nullable = false, length = 64)
    private String cloudEntryHash;

    @Column(name = "synced_payload_hash", nullable = false, length = 64)
    private String syncedPayloadHash;

    @Column(name = "owner_source", nullable = false, length = 20)
    private String ownerSource;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus = "PENDING";

    @Column(name = "tally_xml", columnDefinition = "TEXT")
    private String tallyXml;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
