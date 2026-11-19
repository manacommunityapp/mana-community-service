package com.manacommunity.api.document.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanResultRequest(@NotBlank String result, String details) {}
