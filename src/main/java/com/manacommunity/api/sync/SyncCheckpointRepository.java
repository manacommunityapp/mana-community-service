package com.manacommunity.api.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncCheckpointRepository extends JpaRepository<SyncCheckpoint, Long> {
    Optional<SyncCheckpoint> findByUserIdAndDeviceId(Long userId, String deviceId);
}
