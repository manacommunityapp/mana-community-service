package com.manacommunity.api.mobile.repository;

import com.manacommunity.api.mobile.entity.DeviceTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTelemetryRepository extends JpaRepository<DeviceTelemetry, Long> {

    Optional<DeviceTelemetry> findByDeviceId(String deviceId);

    List<DeviceTelemetry> findByOem(String oem);

    List<DeviceTelemetry> findByRole(DeviceTelemetry.DeviceRole role);

    @Query("SELECT d FROM DeviceTelemetry d WHERE d.coldStartMs > :thresholdMs")
    List<DeviceTelemetry> findSlowStartDevices(long thresholdMs);

    @Query("SELECT d FROM DeviceTelemetry d WHERE d.missedWakeUps > 0 ORDER BY d.missedWakeUps DESC")
    List<DeviceTelemetry> findDevicesWithMissedWakeUps();
}
