package com.manacommunity.api.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GateOfflineDeviceRepository extends JpaRepository<GateOfflineDevice, Long> {
    Optional<GateOfflineDevice> findByCommunityIdAndDeviceId(Long communityId, String deviceId);
}
