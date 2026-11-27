package com.manacommunity.api.devices.repository;

import com.manacommunity.api.devices.entity.DeviceControlLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceControlLogRepository extends JpaRepository<DeviceControlLog, Long> {
    Page<DeviceControlLog> findByDeviceIdOrderByOccurredAtDesc(Long deviceId, Pageable pageable);
    Page<DeviceControlLog> findByActorIdOrderByOccurredAtDesc(Long actorId, Pageable pageable);
}
