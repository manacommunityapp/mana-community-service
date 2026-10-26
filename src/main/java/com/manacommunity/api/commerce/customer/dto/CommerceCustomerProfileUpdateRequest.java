package com.manacommunity.api.commerce.customer.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommerceCustomerProfileUpdateRequest {

    @Size(max = 150)
    private String displayName;

    @Size(max = 20)
    private String phoneNumber;

    private String preferredCategories;
}
