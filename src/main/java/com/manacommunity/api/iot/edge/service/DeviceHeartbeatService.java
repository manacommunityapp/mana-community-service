package com.manacommunity.api.iot.edge.service;

import com.manacommunity.api.iot.edge.entity.DeviceHeartbeatLog;
import com.manacommunity.api.iot.edge.entity.EdgeGatewayDevice;
import com.manacommunity.api.iot.edge.repository.DeviceHeartbeatLogRepository;
import com.manacommunity.api.iot.edge.repository.EdgeGatewayDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceHeartbeatService {

    private final EdgeGatewayDeviceRepository deviceRepository;
    private final DeviceHeartbeatLogRepository heartbeatLogRepository;

    private static final int MISS_THRESHOLD = 2;
    private static final int ISP_OUTAGE_DEVICE_THRESHOLD = 5;
    private static final Duration ISP_OUTAGE_WINDOW = Duration.ofMinutes(5);
    private static final Duration HEARTBEAT_LOG_RETENTION = Duration.ofDays(7);

    private final Set<String> suppressedRegions = ConcurrentHashMap.newKeySet();
    private final Set<String> suppressedIsps = ConcurrentHashMap.newKeySet();

    @Transactional
    public void processHeartbeat(String serialNumber, Integer latencyMs, Integer cpuPct,
                                  Integer memoryPct, Long uptimeSeconds, String firmwareVersion,
                                  Integer signalStrength) {
        EdgeGatewayDevice device = deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Unknown device: " + serialNumber));

        Instant now = Instant.now();
        device.setLastHeartbeatAt(now);
        device.setConsecutiveMisses(0);

        if (!device.getIsOnline()) {
            device.setIsOnline(true);
            device.setOfflineSince(null);
            log.info("Device {} back online after outage", serialNumber);
        }

        if (firmwareVersion != null) {
            device.setFirmwareVersion(firmwareVersion);
        }

        deviceRepository.save(device);

        DeviceHeartbeatLog heartbeatLog = DeviceHeartbeatLog.builder()
                .deviceId(device.getId())
                .communityId(device.getCommunityId())
                .receivedAt(now)
                .latencyMs(latencyMs)
                .cpuUsagePct(cpuPct)
                .memoryUsagePct(memoryPct)
                .uptimeSeconds(uptimeSeconds)
                .firmwareVersion(firmwareVersion)
                .signalStrength(signalStrength)
                .build();
        heartbeatLogRepository.save(heartbeatLog);
    }

    @Transactional
    public SweepResult runOfflineDetectionSweep() {
        Instant now = Instant.now();
        List<EdgeGatewayDevice> staleDevices = deviceRepository.findStaleOnlineDevices(
                now.minus(Duration.ofSeconds(60)));

        List<EdgeGatewayDevice> newlyOffline = new ArrayList<>();
        List<EdgeGatewayDevice> warned = new ArrayList<>();

        for (EdgeGatewayDevice device : staleDevices) {
            int misses = device.getConsecutiveMisses() + 1;
            device.setConsecutiveMisses(misses);

            if (misses >= MISS_THRESHOLD) {
                device.setIsOnline(false);
                device.setOfflineSince(now);
                newlyOffline.add(device);
            } else {
                warned.add(device);
            }
            deviceRepository.save(device);
        }

        List<String> suppressedAlertDevices = new ArrayList<>();
        List<String> alertableDevices = new ArrayList<>();

        detectAndSuppressOutages(now);

        for (EdgeGatewayDevice device : newlyOffline) {
            boolean suppressed = isAlertSuppressed(device);
            if (suppressed) {
                suppressedAlertDevices.add(device.getSerialNumber());
                log.debug("Alert suppressed for {} (ISP/region outage)", device.getSerialNumber());
            } else {
                alertableDevices.add(device.getSerialNumber());
                log.warn("Device {} offline — alerting ops", device.getSerialNumber());
            }
        }

        return new SweepResult(newlyOffline.size(), warned.size(),
                alertableDevices, suppressedAlertDevices);
    }

    private void detectAndSuppressOutages(Instant now) {
        Instant windowStart = now.minus(ISP_OUTAGE_WINDOW);
        suppressedRegions.clear();
        suppressedIsps.clear();

        List<Object[]> regionOutages = deviceRepository.findRegionsWithMassOutage(
                windowStart, ISP_OUTAGE_DEVICE_THRESHOLD);
        for (Object[] row : regionOutages) {
            String region = (String) row[0];
            if (region != null) {
                suppressedRegions.add(region);
                log.warn("Regional outage detected: {} ({} devices offline)", region, row[1]);
            }
        }

        List<Object[]> ispOutages = deviceRepository.findIspOutages(
                windowStart, ISP_OUTAGE_DEVICE_THRESHOLD);
        for (Object[] row : ispOutages) {
            String isp = (String) row[0];
            if (isp != null) {
                suppressedIsps.add(isp);
                log.warn("ISP outage detected: {} ({} devices offline)", isp, row[1]);
            }
        }
    }

    private boolean isAlertSuppressed(EdgeGatewayDevice device) {
        if (device.getRegionTag() != null && suppressedRegions.contains(device.getRegionTag())) {
            return true;
        }
        return device.getIspProvider() != null && suppressedIsps.contains(device.getIspProvider());
    }

    public DeviceHealthSummary getHealthSummary(Long communityId) {
        long total = deviceRepository.countByCommunityIdAndIsOnline(communityId, true)
                + deviceRepository.countByCommunityIdAndIsOnline(communityId, false);
        long online = deviceRepository.countByCommunityIdAndIsOnline(communityId, true);
        long offline = total - online;

        List<EdgeGatewayDevice> offlineDevices = deviceRepository.findByCommunityIdAndIsOnline(communityId, false);
        List<DeviceStatus> offlineStatuses = offlineDevices.stream()
                .map(d -> new DeviceStatus(d.getSerialNumber(), d.getDeviceType(), d.getLocation(),
                        d.getOfflineSince(), d.getConsecutiveMisses()))
                .collect(Collectors.toList());

        return new DeviceHealthSummary(total, online, offline, offlineStatuses,
                new ArrayList<>(suppressedRegions), new ArrayList<>(suppressedIsps));
    }

    @Transactional
    public void purgeOldHeartbeatLogs() {
        heartbeatLogRepository.deleteByReceivedAtBefore(Instant.now().minus(HEARTBEAT_LOG_RETENTION));
    }

    public record SweepResult(int newlyOffline, int warned,
                               List<String> alertableDevices, List<String> suppressedDevices) {}

    public record DeviceStatus(String serialNumber, String deviceType, String location,
                                Instant offlineSince, int consecutiveMisses) {}

    public record DeviceHealthSummary(long totalDevices, long onlineDevices, long offlineDevices,
                                       List<DeviceStatus> offlineDeviceDetails,
                                       List<String> suppressedRegions, List<String> suppressedIsps) {}
}
