package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_external_sync_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsExternalSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false, unique = true)
    private SportsExternalForm form;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_mode", nullable = false, length = 32)
    @Builder.Default
    private SyncMode syncMode = SyncMode.POLLING;

    @Column(name = "watch_id", length = 255)
    private String watchId;

    @Column(name = "watch_resource_id", length = 255)
    private String watchResourceId;

    @Column(name = "watch_expiration")
    private LocalDateTime watchExpiration;

    @Column(name = "last_sync_timestamp")
    private LocalDateTime lastSyncTimestamp;

    @Column(name = "total_responses_polled", nullable = false)
    @Builder.Default
    private Integer totalResponsesPolled = 0;

    @Column(name = "total_imported", nullable = false)
    @Builder.Default
    private Integer totalImported = 0;

    @Column(name = "total_quarantined", nullable = false)
    @Builder.Default
    private Integer totalQuarantined = 0;

    @Column(name = "total_rejected", nullable = false)
    @Builder.Default
    private Integer totalRejected = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.IDLE;

    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onPersist() {
        updatedAt = LocalDateTime.now();
    }

    public enum SyncMode { POLLING, WEBHOOK, PUBSUB }
    public enum SyncStatus { IDLE, SYNCING, ERROR }
}
