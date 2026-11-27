package com.manacommunity.api.iot.edge.repository;

import com.manacommunity.api.iot.edge.entity.DeviceHeartbeatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeviceHeartbeatLogRepository extends JpaRepository<DeviceHeartbeatLog, Long> {

    List<DeviceHeartbeatLog> findByDeviceIdAndReceivedAtAfterOrderByReceivedAtDesc(Long deviceId, Instant after);

    long countByDeviceIdAndReceivedAtAfter(Long deviceId, Instant after);

    void deleteByReceivedAtBefore(Instant before);
}
