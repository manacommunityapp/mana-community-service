package com.manacommunity.api.partnerapi.controller;

import com.manacommunity.api.partnerapi.entity.DeprecatedApiTelemetry;
import com.manacommunity.api.partnerapi.entity.PartnerWebhookKey;
import com.manacommunity.api.partnerapi.service.HardwareSyncGatewayService;
import com.manacommunity.api.partnerapi.service.HardwareSyncGatewayService.*;
import com.manacommunity.api.partnerapi.service.PartnerHmacKeyRotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PartnerApiAndHardwareGatewayController {

    private final PartnerHmacKeyRotationService hmacService;
    private final HardwareSyncGatewayService hardwareGatewayService;

    // ── Dual-Key HMAC Signing & Key Rotation ──────────────────────────────────

    @PostMapping("/api/v1/partners/keys/rotate")
    public ResponseEntity<PartnerWebhookKey> rotateKey(@RequestBody Map<String, Object> body) {
        String partnerId = body.get("partnerId").toString();
        int graceHours = body.containsKey("graceHours") ? Integer.parseInt(body.get("graceHours").toString()) : 168;
        return ResponseEntity.ok(hmacService.issueOrRotateKey(partnerId, graceHours));
    }

    @PostMapping("/api/v1/partners/keys/{keyId}/revoke")
    public ResponseEntity<PartnerWebhookKey> revokeKey(
            @PathVariable String keyId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(hmacService.revokeKeyImmediately(keyId, reason));
    }

    @PostMapping("/api/v1/partners/keys/verify")
    public ResponseEntity<PartnerHmacKeyRotationService.VerificationResult> verifyWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Partner-ID", required = false) String partnerId,
            @RequestHeader(value = "X-Key-ID", required = false) String keyId,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        var result = hmacService.verifyCanonicalRequest(
                partnerId, keyId, "POST", "/api/v1/partners/keys/verify",
                timestamp, nonce, rawPayload, signature, Instant.now());
        HttpStatus status = result.valid() ? HttpStatus.OK
                : ("REPLAY_DETECTED".equals(result.statusCode()) ? HttpStatus.CONFLICT : HttpStatus.UNAUTHORIZED);
        return ResponseEntity.status(status).body(result);
    }

    // ── Hardware API Versioning & RFC 8594 Sunset Gateway ─────────────────────

    @PostMapping("/api/v2/hardware/sync")
    public ResponseEntity<V2HardwareSyncResponse> syncHardwareV2(@RequestBody V2HardwareSyncRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Version", "2.0");
        return ResponseEntity.ok().headers(headers).body(hardwareGatewayService.syncV2(req));
    }

    @PostMapping("/api/v1/hardware/sync")
    public ResponseEntity<?> syncHardwareV1(@RequestBody V1HardwareSyncRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", HardwareSyncGatewayService.V1_SUNSET_HTTP_DATE);
        headers.add("Link", HardwareSyncGatewayService.V2_SUCCESSOR_LINK);
        headers.add("X-API-Version", "1.0");
        try {
            return ResponseEntity.ok().headers(headers).body(hardwareGatewayService.syncV1Compat(req));
        } catch (DeprecatedEndpointSunsetException ex) {
            return ResponseEntity.status(HttpStatus.GONE).headers(headers)
                    .body(Map.of("error", "GONE", "message", ex.getMessage()));
        }
    }

    @GetMapping("/api/v1/partners/deprecation/laggards")
    public ResponseEntity<List<DeprecatedApiTelemetry>> getLaggards() {
        return ResponseEntity.ok(hardwareGatewayService.getDeprecatedLaggards());
    }

    @PutMapping("/api/v1/partners/deprecation/v1-sunset")
    public ResponseEntity<Map<String, Object>> toggleV1Sunset(@RequestBody Map<String, Object> body) {
        boolean enabled = Boolean.parseBoolean(body.getOrDefault("enabled", "false").toString());
        hardwareGatewayService.setV1SunsetEnabled(enabled);
        return ResponseEntity.ok(Map.of("v1SunsetEnabled", hardwareGatewayService.isV1SunsetEnabled()));
    }
}
