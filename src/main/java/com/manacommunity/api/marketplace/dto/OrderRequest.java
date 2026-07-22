package com.manacommunity.api.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull
    private Long listingId;

    private int quantity = 1;

    private String notes;

    private String deliveryAddress;
}
