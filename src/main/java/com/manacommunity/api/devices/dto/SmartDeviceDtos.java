package com.manacommunity.api.devices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class SmartDeviceDtos {

    public record RegisterDeviceRequest(
            @NotNull Long communityId,
            @NotBlank String deviceName,
            @NotBlank String deviceType,
            @NotBlank String location,
            String vendor,
            String model,
            String apiEndpoint,
            String apiKeyRef,
            String firmwareVersion,
            String accessibleBy
    ) {}

    public record DeviceResponse(
            Long id,
            Long communityId,
            String deviceName,
            String deviceType,
            String location,
            String vendor,
            String model,
            String firmwareVersion,
            boolean online,
            Instant lastHeartbeat,
            String accessibleBy,
            Instant createdAt
    ) {}

    public record ControlDeviceRequest(
            @NotBlank String action,
            String requestSource
    ) {}

    public record DeviceControlResult(
            Long logId,
            String result,
            String errorMessage,
            Integer durationMs
    ) {}

    public record DeviceControlLogResponse(
            Long id,
            Long deviceId,
            String action,
            Long actorId,
            String actorRole,
            String requestSource,
            String result,
            String errorMessage,
            Integer durationMs,
            Instant occurredAt
    ) {}

    public record IssueOtpRequest(
            String visitorName,
            int validMinutes
    ) {}

    public record OtpResponse(
            Long id,
            String otpCode,
            String visitorName,
            Instant validFrom,
            Instant validUntil
    ) {}

    public record VerifyOtpRequest(
            @NotBlank String otp
    ) {}
}
