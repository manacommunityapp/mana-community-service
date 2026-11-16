package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SportsStreamCameraRequest {
    @NotBlank
    @Size(max = 100)
    private String label;

    @NotBlank
    @Size(max = 2048)
    private String url;
}
