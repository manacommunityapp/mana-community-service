package com.manacommunity.api.insurance.controller;

import com.manacommunity.api.insurance.service.InsuranceWebhookService;
import com.manacommunity.api.insurance.service.InsuranceWebhookService.Outcome;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Partner webhook endpoint (permitAll in SecurityConfig; authenticity = HMAC signature).
 * Responses are intentionally generic so callers learn nothing about why a request was rejected.
 */
@RestController
@RequestMapping("/api/insurance/webhooks")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "insurance.enabled", havingValue = "true")
public class InsuranceWebhookController {

    private final InsuranceWebhookService service;

    @PostMapping("/{partnerCode}")
    public ResponseEntity<Void> receive(@PathVariable String partnerCode,
                                        @RequestHeader(value = "X-Mana-Signature", required = false) String signature,
                                        @RequestBody(required = false) byte[] body) {
        Outcome o = service.handle(partnerCode, signature, body == null ? new byte[0] : body);
        return switch (o) {
            case PROCESSED, DUPLICATE -> ResponseEntity.accepted().build();
            case REJECTED_PAYLOAD -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            default -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        };
    }
}
