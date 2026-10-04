package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SmartMeterRequest(
    @NotBlank String flatNumber,
    @NotNull String meterType,
    @NotBlank String meterSerial,
    String location
) {}
