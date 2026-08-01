package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterProviderRequest {
    @NotBlank
    private String providerType;
    @Size(max = 200)
    private String businessName;
    @Size(max = 20)
    private String phone;
    @Size(max = 100)
    private String email;
    private String bio;
    private String profileImageUrl;
    private String serviceAreas;
    private String certifications;
    private Long vendorId;
}
