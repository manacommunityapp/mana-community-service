package com.manacommunity.api.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String borrowedBy;
    private String borrowedByFlat;
    private String expectedReturnAt;
}
