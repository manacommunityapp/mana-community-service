package com.manacommunity.api.commerce.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommerceCustomerAddressRequest {

    /** HOME, WORK, OTHER */
    private String label;

    @NotBlank
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 150)
    private String landmark;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    private String state;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String pincode;

    private Double latitude;
    private Double longitude;
    private Boolean isDefault;
}
