package com.manacommunity.api.iot.mesh.repository;

import com.manacommunity.api.iot.mesh.entity.IoTSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IoTSensorRepository extends JpaRepository<IoTSensor, Long> {
    List<IoTSensor> findByCommunityIdAndActiveTrue(Long communityId);
    List<IoTSensor> findByCommunityIdAndSensorTypeAndActiveTrue(Long communityId, String sensorType);
    List<IoTSensor> findByActiveTrue();
}
