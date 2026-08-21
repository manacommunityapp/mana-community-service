package com.manacommunity.api.controller;

import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal endpoint for service-to-service SMS dispatch.
 * Requires SUPER_ADMIN authority or a trusted internal token.
 */
@RestController
@RequestMapping("/api/v1/internal/notifications/sms")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class InternalSmsController {

    private final SmsService smsService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> send(@Valid @RequestBody SendSmsRequest request) {
        Long messageId = smsService.send(request);
        return ResponseEntity.ok(Map.of("messageId", messageId != null ? messageId : -1L));
    }
}
