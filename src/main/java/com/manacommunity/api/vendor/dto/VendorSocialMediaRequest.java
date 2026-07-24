package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorSocialMediaRequest {
    @NotBlank
    private String platform;
    @NotBlank
    private String profileUrl;
}
