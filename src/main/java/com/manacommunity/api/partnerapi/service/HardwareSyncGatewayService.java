package com.manacommunity.api.partnerapi.service;

import com.manacommunity.api.partnerapi.entity.DeprecatedApiTelemetry;
import com.manacommunity.api.partnerapi.repository.DeprecatedApiTelemetryRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hardware Controller API Gateway & Versioning Transformer:
 * Maintains one canonical v2 business core while translating legacy /v1/hardware/sync
 * snake-case payloads cleanly with RFC 8594 Deprecation/Sunset headers and controller laggard telemetry.
 */
@Service
@RequiredArgsConstructor
public class HardwareSyncGatewayService {

    public static final String V1_ENDPOINT = "/api/v1/hardware/sync";
    public static final String V2_SUCCESSOR_LINK = "</api/v2/hardware/sync>; rel=\"successor-version\"";
    public static final String V1_SUNSET_HTTP_DATE = "Wed, 30 Sep 2027 23:59:59 GMT";

    private final DeprecatedApiTelemetryRepository telemetryRepo;
    private final AtomicBoolean v1SunsetEnabled = new AtomicBoolean(false);

    @Builder
    public record V1HardwareSyncRequest(
            String device_sn,
            Long community_id,
            String fw_ver,
            Long last_sync_epoch,
            List<Map<String, Object>> card_events
    ) {}

    @Builder
    public record V2HardwareSyncRequest(
            String controllerSerial,
            Long communityId,
            String firmwareVersion,
            long sinceVersion,
            List<Map<String, Object>> events
    ) {}

    @Builder
    public record V2HardwareSyncResponse(
            String apiVersion,
            String controllerSerial,
            long cursorVersion,
            int acceptedEventsCount,
            String serverTimestamp,
            List<Map<String, Object>> actions
    ) {}

    @Builder
    public record V1HardwareSyncResponse(
            int ok,
            long ts,
            String dev_sn,
            long next_cursor,
            List<Map<String, Object>> actions
    ) {}

    public boolean isV1SunsetEnabled() {
        return v1SunsetEnabled.get();
    }

    public void setV1SunsetEnabled(boolean enabled) {
        this.v1SunsetEnabled.set(enabled);
    }

    public V2HardwareSyncResponse syncV2(V2HardwareSyncRequest req) {
        Instant now = Instant.now();
        int count = req.events() != null ? req.events().size() : 0;
        long nextCursor = Math.max(1L, req.sinceVersion() + Math.max(1, count));
        List<Map<String, Object>> actions = List.of(
                Map.of(
                        "actionType", "ALLOWLIST_DELTA",
                        "zoneId", "MAIN_GATE",
                        "cursorVersion", nextCursor
                )
        );
        return V2HardwareSyncResponse.builder()
                .apiVersion("2.0")
                .controllerSerial(req.controllerSerial())
                .cursorVersion(nextCursor)
                .acceptedEventsCount(count)
                .serverTimestamp(now.toString())
                .actions(actions)
                .build();
    }

    @Transactional
    public V1HardwareSyncResponse syncV1Compat(V1HardwareSyncRequest v1Req) {
        if (v1SunsetEnabled.get()) {
            throw new DeprecatedEndpointSunsetException(
                    "Endpoint /api/v1/hardware/sync has reached sunset; upgrade controller firmware to /api/v2/hardware/sync");
        }

        String serial = (v1Req.device_sn() != null && !v1Req.device_sn().isBlank())
                ? v1Req.device_sn().trim()
                : "UNKNOWN-SERIAL";

        // Record lag telemetry per physical field controller
        DeprecatedApiTelemetry tel = telemetryRepo.findByEndpointAndControllerSerial(V1_ENDPOINT, serial)
                .orElseGet(() -> DeprecatedApiTelemetry.builder()
                        .endpoint(V1_ENDPOINT)
                        .apiVersion("1.0")
                        .controllerSerial(serial)
                        .callCount(0L)
                        .build());
        tel.setCommunityId(v1Req.community_id());
        tel.setFirmwareVersion(v1Req.fw_ver());
        tel.setCallCount(tel.getCallCount() + 1);
        tel.setLastSeenAt(Instant.now());
        telemetryRepo.save(tel);

        // Upcast V1 -> V2 canonical request
        long sinceVer = v1Req.last_sync_epoch() != null ? Math.max(0L, v1Req.last_sync_epoch() % 1_000_000L) : 0L;
        V2HardwareSyncResponse v2Res = syncV2(V2HardwareSyncRequest.builder()
                .controllerSerial(serial)
                .communityId(v1Req.community_id())
                .firmwareVersion(v1Req.fw_ver())
                .sinceVersion(sinceVer)
                .events(v1Req.card_events())
                .build());

        // Downcast V2 -> compact V1 payload expected by embedded hardware firmware
        List<Map<String, Object>> compactActions = v2Res.actions().stream()
                .map(a -> Map.<String, Object>of(
                        "cmd", a.getOrDefault("actionType", "ALLOWLIST_DELTA"),
                        "cur", v2Res.cursorVersion()
                ))
                .toList();

        return V1HardwareSyncResponse.builder()
                .ok(1)
                .ts(Instant.now().getEpochSecond())
                .dev_sn(serial)
                .next_cursor(v2Res.cursorVersion())
                .actions(compactActions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DeprecatedApiTelemetry> getDeprecatedLaggards() {
        return telemetryRepo.findByEndpointOrderByCallCountDesc(V1_ENDPOINT);
    }

    public static class DeprecatedEndpointSunsetException extends RuntimeException {
        public DeprecatedEndpointSunsetException(String message) {
            super(message);
        }
    }
}
