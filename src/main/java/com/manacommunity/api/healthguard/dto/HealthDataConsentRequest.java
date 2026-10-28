package com.manacommunity.api.healthguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HealthDataConsentRequest {
    @NotBlank
    private String provider;

    @NotBlank
    @Size(max = 40)
    private String consentVersion;

    @NotEmpty
    @Size(max = 8)
    private List<@NotBlank String> grantedScopes;
}
