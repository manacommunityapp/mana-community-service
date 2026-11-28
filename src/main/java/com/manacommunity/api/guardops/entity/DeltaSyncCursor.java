package com.manacommunity.api.guardops.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delta_sync_cursors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeltaSyncCursor extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private String guardId;
    private String communityId;

    @Enumerated(EnumType.STRING)
    private SyncEntity syncEntity;

    private String lastCursor;
    private LocalDateTime lastSyncAt;
    private long recordsSynced;
    private long tombstonesPurged;
    private long bytesTransferred;
    private long bloomFilterSizeBytes;
    private double bloomFilterFalsePositiveRate;

    private long totalDeviceStorageMb;
    private long usedCacheMb;
    private long evictedRecords;

    @Enumerated(EnumType.STRING)
    private SyncStatus status;

    public enum SyncEntity { RESIDENTS, VEHICLES, DAILY_HELP, VISITORS_EXPECTED, PACKAGES_PENDING, INTERCOM_DIRECTORY, EMERGENCY_CONTACTS }
    public enum SyncStatus { UP_TO_DATE, SYNCING, STALE, FULL_RESYNC_NEEDED, ERROR }
}
