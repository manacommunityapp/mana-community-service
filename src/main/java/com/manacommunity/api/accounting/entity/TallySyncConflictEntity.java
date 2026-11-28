package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tally_sync_conflicts", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class TallySyncConflictEntity {

    @Id
    @Column(name = "conflict_id", nullable = false)
    private UUID conflictId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "tally_guid", nullable = false)
    private UUID tallyGuid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cloud_snapshot_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> cloudSnapshotJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tally_snapshot_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> tallySnapshotJson;

    @Column(name = "resolution_status", nullable = false, length = 30)
    private String resolutionStatus = "OPEN";

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
