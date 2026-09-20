package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommuteFavouriteRouteRequest {
    @NotBlank
    private String label;
    @NotBlank
    private String fromLocation;
    @NotBlank
    private String toLocation;
    private Double fromLat;
    private Double fromLng;
    private Double toLat;
    private Double toLng;
}
