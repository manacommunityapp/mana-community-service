package com.manacommunity.api.controller;

import com.manacommunity.api.notification.dto.SmsOtpRequest;
import com.manacommunity.api.notification.dto.SmsOtpResponse;
import com.manacommunity.api.notification.dto.SmsOtpVerifyRequest;
import com.manacommunity.api.notification.service.SmsOtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp/phone")
@RequiredArgsConstructor
public class SmsOtpController {

    private final SmsOtpService smsOtpService;

    @PostMapping("/send")
    public ResponseEntity<SmsOtpResponse> sendOtp(
            @Valid @RequestBody SmsOtpRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        return ResponseEntity.ok(smsOtpService.sendOtp(request, clientIp));
    }

    @PostMapping("/verify")
    public ResponseEntity<SmsOtpResponse> verifyOtp(@Valid @RequestBody SmsOtpVerifyRequest request) {
        return ResponseEntity.ok(smsOtpService.verifyOtp(request));
    }

    private String extractClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
