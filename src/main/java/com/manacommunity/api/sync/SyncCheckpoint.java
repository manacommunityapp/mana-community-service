package com.manacommunity.api.sync;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_checkpoints", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "device_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Builder.Default
    @Column(name = "last_synced_change_id", nullable = false)
    private Long lastSyncedChangeId = 0L;

    @Builder.Default
    @Column(name = "last_sync_timestamp", nullable = false)
    private LocalDateTime lastSyncTimestamp = LocalDateTime.now();

    @Column(name = "client_app_version", length = 50)
    private String clientAppVersion;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
