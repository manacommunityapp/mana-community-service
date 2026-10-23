package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvTelemetryHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvTelemetryHeartbeatRepository extends JpaRepository<EvTelemetryHeartbeat, Long> {
    List<EvTelemetryHeartbeat> findTop20ByChargerIdOrderByTimestampDesc(Long chargerId);
    List<EvTelemetryHeartbeat> findBySessionIdOrderByTimestampAsc(Long sessionId);
}
