package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.WearableDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WearableDeviceRepository extends JpaRepository<WearableDevice, Long> {
    List<WearableDevice> findByUserIdAndIsActiveTrueOrderByRegisteredAtDesc(Long userId);
    Optional<WearableDevice> findByIdAndUserId(Long id, Long userId);
    Optional<WearableDevice> findByUserIdAndDeviceIdentifier(Long userId, String deviceIdentifier);
}
