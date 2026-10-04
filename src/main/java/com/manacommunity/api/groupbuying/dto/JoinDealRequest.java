package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinDealRequest {
    @NotNull
    @Min(1)
    private Integer quantity;
}
