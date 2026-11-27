package com.manacommunity.api.grouporder.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class JoinGroupOrderRequest {
    @Min(1)
    private int quantity = 1;
}
