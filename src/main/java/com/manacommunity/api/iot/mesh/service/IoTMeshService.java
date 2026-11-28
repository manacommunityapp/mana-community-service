package com.manacommunity.api.iot.mesh.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.iot.mesh.dto.IoTMeshDtos.*;
import com.manacommunity.api.iot.mesh.entity.IoTSensor;
import com.manacommunity.api.iot.mesh.entity.SensorAlertConfig;
import com.manacommunity.api.iot.mesh.entity.SensorAnomalyEvent;
import com.manacommunity.api.iot.mesh.repository.IoTSensorRepository;
import com.manacommunity.api.iot.mesh.repository.SensorAlertConfigRepository;
import com.manacommunity.api.iot.mesh.repository.SensorAnomalyEventRepository;
import com.manacommunity.api.iot.mesh.sqs.IoTAlertSqsHandler;
import com.manacommunity.api.iot.mesh.sqs.IoTAlertSqsHandler.IoTAnomalyPushPayload;
import com.manacommunity.platform.sqs.SqsMessagePublisher;
import com.manacommunity.platform.sqs.SqsProperties;
import com.manacommunity.platform.websocket.IoTAlertsRedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IoTMeshService {

    private final IoTSensorRepository sensorRepo;
    private final SensorAlertConfigRepository alertConfigRepo;
    private final SensorAnomalyEventRepository anomalyRepo;
    private final SimpMessagingTemplate ws;
    private final JdbcTemplate jdbc;

    /** Redis Pub/Sub publisher — optional, falls back to local STOMP when null. */
    @Autowired(required = false)
    private IoTAlertsRedisPublisher iotAlertsRedisPublisher;

    @Autowired(required = false)
    private SqsMessagePublisher sqsMessagePublisher;
    @Autowired(required = false)
    private SqsProperties sqsProperties;
    @Autowired(required = false)
    private IoTAlertSqsHandler iotAlertSqsHandler;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static String sensorReadingKey(Long communityId, Long sensorId) {
        String scope = communityId != null ? String.valueOf(communityId) : "global";
        return "iot:comm:" + scope + ":sensor:" + sensorId + LAST_READING_KEY_SUFFIX;
    }
    private static final String LAST_READING_KEY_SUFFIX = ":last-reading";
    private static final java.time.Duration LAST_READING_TTL = java.time.Duration.ofMinutes(10);

    // ── Registry ──────────────────────────────────────────────────────────

    @Transactional
    public IoTSensor registerSensor(RegisterSensorRequest req) {
        IoTSensor sensor = IoTSensor.builder()
                .communityId(req.communityId())
                .sensorName(req.sensorName())
                .sensorType(req.sensorType())
                .location(req.location())
                .unit(req.unit())
                .vendor(req.vendor())
                .serialNumber(req.serialNumber())
                .apiEndpoint(req.apiEndpoint())
                .apiKeyRef(req.apiKeyRef())
                .pollingIntervalSeconds(req.pollingIntervalSeconds() != null ? req.pollingIntervalSeconds() : 300)
                .build();
        return sensorRepo.save(sensor);
    }

    @Transactional
    public IoTSensor updateSensor(Long sensorId, RegisterSensorRequest req) {
        IoTSensor sensor = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor not found: " + sensorId));
        sensor.setSensorName(req.sensorName());
        sensor.setLocation(req.location());
        sensor.setVendor(req.vendor());
        if (req.pollingIntervalSeconds() != null) {
            sensor.setPollingIntervalSeconds(req.pollingIntervalSeconds());
        }
        return sensorRepo.save(sensor);
    }

    public List<IoTSensor> getSensors(Long communityId, String type) {
        if (type != null && !type.isBlank()) {
            return sensorRepo.findByCommunityIdAndSensorTypeAndActiveTrue(communityId, type.toUpperCase());
        }
        return sensorRepo.findByCommunityIdAndActiveTrue(communityId);
    }

    // ── Telemetry ─────────────────────────────────────────────────────────

    @Transactional
    public TelemetryPoint ingestReading(Long sensorId, BigDecimal value, Instant readingAt, String quality) {
        IoTSensor sensor = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor not found: " + sensorId));

        Instant ts = readingAt != null ? readingAt : Instant.now();
        String q = quality != null ? quality : "GOOD";

        jdbc.update(
                "INSERT INTO sensor_telemetry(sensor_id, community_id, reading_value, reading_at, quality) VALUES (?,?,?,?,?)",
                sensorId, sensor.getCommunityId(), value, ts, q
        );

        sensor.setLastReading(value);
        sensor.setLastReadingAt(ts);
        sensorRepo.save(sensor);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(
                        sensorReadingKey(sensor.getCommunityId(), sensorId),
                        value.toPlainString(),
                        LAST_READING_TTL
                );
            } catch (Exception e) {
                log.debug("Redis write failed for sensor last-reading sensorId={}: {}", sensorId, e.getMessage());
            }
        }

        checkAlerts(sensor, value);

        return new TelemetryPoint(value, ts, q);
    }

    private void checkAlerts(IoTSensor sensor, BigDecimal value) {
        List<SensorAlertConfig> configs = alertConfigRepo.findBySensorIdAndActiveTrue(sensor.getId());
        for (SensorAlertConfig cfg : configs) {
            boolean triggered = false;
            String desc = null;

            if ("THRESHOLD_HIGH".equals(cfg.getAlertType()) && cfg.getThresholdValue() != null) {
                if (value.compareTo(cfg.getThresholdValue()) > 0) {
                    triggered = true;
                    desc = sensor.getSensorName() + " reading " + value + " exceeded high threshold " + cfg.getThresholdValue();
                }
            } else if ("THRESHOLD_LOW".equals(cfg.getAlertType()) && cfg.getThresholdValue() != null) {
                if (value.compareTo(cfg.getThresholdValue()) < 0) {
                    triggered = true;
                    desc = sensor.getSensorName() + " reading " + value + " below low threshold " + cfg.getThresholdValue();
                }
            }

            if (triggered) {
                SensorAnomalyEvent event = SensorAnomalyEvent.builder()
                        .sensorId(sensor.getId())
                        .communityId(sensor.getCommunityId())
                        .alertConfigId(cfg.getId())
                        .eventType(cfg.getAlertType())
                        .detectedValue(value)
                        .thresholdValue(cfg.getThresholdValue())
                        .severity(cfg.getSeverity())
                        .description(desc)
                        .build();
                anomalyRepo.save(event);

                Map<String, Object> alertPayload = Map.of(
                        "type", "ANOMALY",
                        "sensorId", sensor.getId(),
                        "severity", cfg.getSeverity(),
                        "description", desc
                );
                if (iotAlertsRedisPublisher != null) {
                    // Redis Pub/Sub — delivers to all nodes
                    iotAlertsRedisPublisher.publishAlert(sensor.getCommunityId(), alertPayload);
                } else {
                    // Fallback: local in-memory broadcast
                    ws.convertAndSend(
                            "/topic/community/" + sensor.getCommunityId() + "/iot-feed",
                            alertPayload
                    );
                }
                if (sqsMessagePublisher != null && sqsProperties != null) {
                    var pushPayload = new IoTAnomalyPushPayload(
                            sensor.getCommunityId(), sensor.getId(), sensor.getSensorName(),
                            cfg.getSeverity(), desc);
                    sqsMessagePublisher.publishAfterCommit(
                            sqsProperties.getQueues().getIotAlerts(),
                            "SENSOR_ANOMALY",
                            pushPayload,
                            iotAlertSqsHandler != null ? () -> iotAlertSqsHandler.deliverPush(pushPayload) : null
                    );
                }
                log.warn("IoT anomaly: communityId={} sensor={} {}", sensor.getCommunityId(), sensor.getId(), desc);
            }
        }
    }

    // ── Polling ───────────────────────────────────────────────────────────

    public BigDecimal pollSensor(Long sensorId) {
        Long activeCommunityId = com.manacommunity.api.config.tenant.TenantContext.getCommunityId();
        if (redisTemplate != null && sensorId != null && activeCommunityId != null) {
            try {
                String cached = redisTemplate.opsForValue().get(
                        sensorReadingKey(activeCommunityId, sensorId));
                if (cached != null && !cached.isBlank()) {
                    return new BigDecimal(cached);
                }
            } catch (Exception e) {
                log.debug("Redis read failed for sensor last-reading sensorId={}: {}", sensorId, e.getMessage());
            }
        }
        IoTSensor sensor = sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor not found: " + sensorId));
        if (activeCommunityId != null && !activeCommunityId.equals(sensor.getCommunityId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Sensor " + sensorId + " does not belong to active community " + activeCommunityId);
        }
        // Stub: return last reading or 0
        BigDecimal value = sensor.getLastReading() != null ? sensor.getLastReading() : BigDecimal.ZERO;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(
                        sensorReadingKey(sensor.getCommunityId(), sensorId),
                        value.toPlainString(),
                        LAST_READING_TTL
                );
            } catch (Exception ignored) {}
        }
        log.info("STUB poll sensor={} returning {}", sensorId, value);
        return value;
    }

    @Scheduled(fixedRate = 300_000)
    public void pollAllSensors() {
        List<IoTSensor> active = sensorRepo.findByActiveTrue();
        log.debug("IoT mesh scheduled poll: {} sensors", active.size());
        for (IoTSensor sensor : active) {
            try {
                BigDecimal reading = pollSensor(sensor.getId());
                ingestReading(sensor.getId(), reading, Instant.now(), "ESTIMATED");
            } catch (Exception e) {
                log.warn("Poll failed for sensor {}: {}", sensor.getId(), e.getMessage());
            }
        }
    }

    // ── Analytics ─────────────────────────────────────────────────────────

    public List<TelemetryPoint> getTelemetryTrend(Long sensorId, int hours) {
        Instant since = Instant.now().minusSeconds((long) hours * 3600);
        return jdbc.query(
                "SELECT reading_value, reading_at, quality FROM sensor_telemetry " +
                "WHERE sensor_id = ? AND reading_at >= ? ORDER BY reading_at",
                (rs, row) -> new TelemetryPoint(
                        rs.getBigDecimal("reading_value"),
                        rs.getTimestamp("reading_at").toInstant(),
                        rs.getString("quality")
                ),
                sensorId, java.sql.Timestamp.from(since)
        );
    }

    public List<DailyAggregate> getDailyAggregates(Long sensorId, int days) {
        Instant since = Instant.now().minusSeconds((long) days * 86400);
        return jdbc.query(
                "SELECT DATE(reading_at) AS day, MIN(reading_value), MAX(reading_value), " +
                "AVG(reading_value), SUM(reading_value), COUNT(*) " +
                "FROM sensor_telemetry WHERE sensor_id = ? AND reading_at >= ? " +
                "GROUP BY DATE(reading_at) ORDER BY day",
                (rs, row) -> new DailyAggregate(
                        rs.getDate("day").toLocalDate(),
                        rs.getBigDecimal("min"),
                        rs.getBigDecimal("max"),
                        rs.getBigDecimal("avg"),
                        rs.getBigDecimal("sum"),
                        rs.getLong("count")
                ),
                sensorId, java.sql.Timestamp.from(since)
        );
    }

    // ── Anomaly management ────────────────────────────────────────────────

    public List<SensorAnomalyEvent> getActiveAnomalies(Long communityId) {
        return anomalyRepo.findByCommunityIdAndResolvedAtIsNullOrderByDetectedAtDesc(communityId);
    }

    @Transactional
    public SensorAnomalyEvent resolveAnomaly(Long anomalyId, Long resolvedBy) {
        SensorAnomalyEvent event = anomalyRepo.findById(anomalyId)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly event not found: " + anomalyId));
        event.setResolvedAt(Instant.now());
        event.setResolvedBy(resolvedBy);
        return anomalyRepo.save(event);
    }

    @Transactional
    public SensorAlertConfig configureAlert(Long sensorId, AlertConfigRequest req) {
        sensorRepo.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor not found: " + sensorId));
        SensorAlertConfig cfg = SensorAlertConfig.builder()
                .sensorId(sensorId)
                .alertType(req.alertType())
                .thresholdValue(req.thresholdValue())
                .rateChangePct(req.rateChangePct())
                .windowMinutes(req.windowMinutes() != null ? req.windowMinutes() : 60)
                .severity(req.severity() != null ? req.severity() : "WARNING")
                .notifyRoles(req.notifyRoles() != null ? req.notifyRoles() : "FM,ADMIN")
                .build();
        return alertConfigRepo.save(cfg);
    }

    // ── Emergency broadcast ───────────────────────────────────────────────

    public void broadcastEmergency(Long communityId, String message, String severity) {
        log.warn("EMERGENCY broadcast communityId={} severity={}: {}", communityId, severity, message);
        ws.convertAndSend(
                "/topic/emergency/" + communityId,
                Map.of("type", "EMERGENCY", "message", message, "severity", severity,
                       "broadcastAt", Instant.now().toString())
        );
    }
}
