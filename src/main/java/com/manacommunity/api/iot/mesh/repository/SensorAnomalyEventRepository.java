package com.manacommunity.api.iot.mesh.repository;

import com.manacommunity.api.iot.mesh.entity.SensorAnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorAnomalyEventRepository extends JpaRepository<SensorAnomalyEvent, Long> {
    List<SensorAnomalyEvent> findByCommunityIdAndResolvedAtIsNullOrderByDetectedAtDesc(Long communityId);
    List<SensorAnomalyEvent> findBySensorIdOrderByDetectedAtDesc(Long sensorId);
}
