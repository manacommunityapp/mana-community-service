package com.manacommunity.api.unit.service;

import com.manacommunity.api.partnerapi.entity.DeprecatedApiTelemetry;
import com.manacommunity.api.partnerapi.entity.PartnerWebhookKey;
import com.manacommunity.api.partnerapi.entity.WebhookNonceJournal;
import com.manacommunity.api.partnerapi.repository.DeprecatedApiTelemetryRepository;
import com.manacommunity.api.partnerapi.repository.PartnerWebhookKeyRepository;
import com.manacommunity.api.partnerapi.repository.WebhookNonceJournalRepository;
import com.manacommunity.api.partnerapi.service.HardwareSyncGatewayService;
import com.manacommunity.api.partnerapi.service.HardwareSyncGatewayService.DeprecatedEndpointSunsetException;
import com.manacommunity.api.partnerapi.service.HardwareSyncGatewayService.V1HardwareSyncRequest;
import com.manacommunity.api.partnerapi.service.HardwareSyncGatewayService.V1HardwareSyncResponse;
import com.manacommunity.api.partnerapi.service.PartnerHmacKeyRotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PartnerHmacAndHardwareVersioningTest {

    @Mock
    private PartnerWebhookKeyRepository keyRepo;

    @Mock
    private WebhookNonceJournalRepository nonceRepo;

    @Mock
    private DeprecatedApiTelemetryRepository telemetryRepo;

    private PartnerHmacKeyRotationService hmacService;
    private HardwareSyncGatewayService gatewayService;

    private final List<PartnerWebhookKey> keyStore = new ArrayList<>();
    private final Map<String, WebhookNonceJournal> nonceStore = new ConcurrentHashMap<>();
    private final Map<String, DeprecatedApiTelemetry> telemetryStore = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        keyStore.clear();
        nonceStore.clear();
        telemetryStore.clear();

        lenient().when(keyRepo.save(any())).thenAnswer(inv -> {
            PartnerWebhookKey k = inv.getArgument(0);
            if (k.getId() == null) {
                k.setId(idSeq.getAndIncrement());
                keyStore.add(k);
            }
            return k;
        });
        lenient().when(keyRepo.findByPartnerIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv -> {
            String p = inv.getArgument(0);
            return keyStore.stream().filter(k -> p.equals(k.getPartnerId())).toList();
        });
        lenient().when(keyRepo.findByKeyId(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return keyStore.stream().filter(k -> id.equals(k.getKeyId())).findFirst();
        });
        lenient().when(keyRepo.findFirstByPartnerIdAndStatusOrderByCreatedAtDesc(anyString(), anyString())).thenAnswer(inv -> {
            String p = inv.getArgument(0);
            String st = inv.getArgument(1);
            return keyStore.stream().filter(k -> p.equals(k.getPartnerId()) && st.equals(k.getStatus())).findFirst();
        });

        lenient().when(nonceRepo.findByPartnerIdAndNonce(anyString(), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0) + ":" + inv.getArgument(1);
            return Optional.ofNullable(nonceStore.get(key));
        });
        lenient().when(nonceRepo.save(any())).thenAnswer(inv -> {
            WebhookNonceJournal j = inv.getArgument(0);
            nonceStore.put(j.getPartnerId() + ":" + j.getNonce(), j);
            return j;
        });

        lenient().when(telemetryRepo.findByEndpointAndControllerSerial(anyString(), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0) + "|" + inv.getArgument(1);
            return Optional.ofNullable(telemetryStore.get(key));
        });
        lenient().when(telemetryRepo.save(any())).thenAnswer(inv -> {
            DeprecatedApiTelemetry t = inv.getArgument(0);
            telemetryStore.put(t.getEndpoint() + "|" + t.getControllerSerial(), t);
            return t;
        });
        lenient().when(telemetryRepo.findByEndpointOrderByCallCountDesc(anyString())).thenAnswer(inv -> {
            String ep = inv.getArgument(0);
            return telemetryStore.values().stream()
                    .filter(t -> ep.equals(t.getEndpoint()))
                    .sorted(Comparator.comparingLong(DeprecatedApiTelemetry::getCallCount).reversed())
                    .toList();
        });

        hmacService = new PartnerHmacKeyRotationService(keyRepo, nonceRepo);
        gatewayService = new HardwareSyncGatewayService(telemetryRepo);
    }

    @Test
    void supportsDualKeyRotationGraceWindowReplayDefenseAndInstantRevocation() {
        PartnerWebhookKey k1 = hmacService.issueOrRotateKey("BOOM-BARRIER", 168);
        assertThat(k1.getStatus()).isEqualTo("ACTIVE");

        PartnerWebhookKey k2 = hmacService.issueOrRotateKey("BOOM-BARRIER", 168);
        assertThat(k2.getStatus()).isEqualTo("ACTIVE");
        assertThat(k1.getStatus()).isEqualTo("ROTATING_GRACE");

        Instant now = Instant.parse("2026-11-28T10:00:00Z");
        String body = "{\"gate\":\"G1\",\"cmd\":\"OPEN\"}";
        String path = "/api/v1/partners/keys/verify";

        // Both k1 (ROTATING_GRACE) and k2 (ACTIVE) verify cleanly during grace overlap
        String canonical1 = PartnerHmacKeyRotationService.buildCanonicalPayload("POST", path, now.getEpochSecond(), "nonce-k1", body);
        String sig1 = "sha256=" + PartnerHmacKeyRotationService.signCanonical(k1.getSecretHex(), canonical1);
        var res1 = hmacService.verifyCanonicalRequest("BOOM-BARRIER", k1.getKeyId(), "POST", path,
                String.valueOf(now.getEpochSecond()), "nonce-k1", body, sig1, now);
        assertThat(res1.valid()).isTrue();
        assertThat(res1.keyStatus()).isEqualTo("ROTATING_GRACE");

        // Replaying the same nonce inside the drift window is rejected
        var replay = hmacService.verifyCanonicalRequest("BOOM-BARRIER", k1.getKeyId(), "POST", path,
                String.valueOf(now.getEpochSecond()), "nonce-k1", body, sig1, now.plusSeconds(10));
        assertThat(replay.valid()).isFalse();
        assertThat(replay.statusCode()).isEqualTo("REPLAY_DETECTED");

        // Timestamp outside 300s tolerance window is rejected
        var expired = hmacService.verifyCanonicalRequest("BOOM-BARRIER", k2.getKeyId(), "POST", path,
                String.valueOf(now.minusSeconds(350).getEpochSecond()), "nonce-expired", body, sig1, now);
        assertThat(expired.valid()).isFalse();
        assertThat(expired.statusCode()).isEqualTo("TIMESTAMP_EXPIRED");

        // Immediate revocation quarantines compromised k1 while k2 remains operational
        hmacService.revokeKeyImmediately(k1.getKeyId(), "COMPROMISED_GATE_CONTROLLER");
        String canonicalAfterRevoke = PartnerHmacKeyRotationService.buildCanonicalPayload("POST", path, now.getEpochSecond(), "nonce-k1-b", body);
        String sigRevoked = "sha256=" + PartnerHmacKeyRotationService.signCanonical(k1.getSecretHex(), canonicalAfterRevoke);
        var revokedCheck = hmacService.verifyCanonicalRequest("BOOM-BARRIER", k1.getKeyId(), "POST", path,
                String.valueOf(now.getEpochSecond()), "nonce-k1-b", body, sigRevoked, now);
        assertThat(revokedCheck.valid()).isFalse();
        assertThat(revokedCheck.statusCode()).isEqualTo("KEY_REVOKED");

        // Outbound signing automatically uses newly ACTIVE k2
        Map<String, String> outbound = hmacService.signOutboundWebhook("BOOM-BARRIER", "POST", path, body, now);
        assertThat(outbound.get("X-Key-ID")).isEqualTo(k2.getKeyId());
        var verifiedOutbound = hmacService.verifyCanonicalRequest("BOOM-BARRIER", outbound.get("X-Key-ID"),
                "POST", path, outbound.get("X-Timestamp"), outbound.get("X-Nonce"), body, outbound.get("X-Signature"), now);
        assertThat(verifiedOutbound.valid()).isTrue();
    }

    @Test
    void upcastsLegacyV1HardwareSyncRecordsLaggardTelemetryAndEnforcesSunset() {
        V1HardwareSyncRequest v1Req = V1HardwareSyncRequest.builder()
                .device_sn("CTRL-99")
                .community_id(412L)
                .fw_ver("v1.4.2-legacy")
                .last_sync_epoch(1760000105L)
                .card_events(List.of(Map.of("rfid", "TAG-001"), Map.of("rfid", "TAG-002")))
                .build();

        V1HardwareSyncResponse v1Res = gatewayService.syncV1Compat(v1Req);
        assertThat(v1Res.ok()).isEqualTo(1);
        assertThat(v1Res.dev_sn()).isEqualTo("CTRL-99");
        assertThat(v1Res.next_cursor()).isGreaterThan(0L);
        assertThat(v1Res.actions()).hasSize(1);
        assertThat(v1Res.actions().get(0).get("cmd")).isEqualTo("ALLOWLIST_DELTA");

        List<DeprecatedApiTelemetry> laggards = gatewayService.getDeprecatedLaggards();
        assertThat(laggards).hasSize(1);
        assertThat(laggards.get(0).getControllerSerial()).isEqualTo("CTRL-99");
        assertThat(laggards.get(0).getCallCount()).isEqualTo(1L);

        // Enabling V1 sunset rejects legacy callers with 410 Gone exception
        gatewayService.setV1SunsetEnabled(true);
        assertThatThrownBy(() -> gatewayService.syncV1Compat(v1Req))
                .isInstanceOf(DeprecatedEndpointSunsetException.class)
                .hasMessageContaining("/api/v2/hardware/sync");
    }
}
