package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTripStatusRequest {
    @NotBlank private String status;
}
