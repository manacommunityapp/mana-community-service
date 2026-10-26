package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class JoinCarpoolRequest {
    @Min(1) private int seats = 1;
}
