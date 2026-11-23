package com.manacommunity.api.iot.mesh.repository;

import com.manacommunity.api.iot.mesh.entity.SensorAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorAlertConfigRepository extends JpaRepository<SensorAlertConfig, Long> {
    List<SensorAlertConfig> findBySensorIdAndActiveTrue(Long sensorId);
}
