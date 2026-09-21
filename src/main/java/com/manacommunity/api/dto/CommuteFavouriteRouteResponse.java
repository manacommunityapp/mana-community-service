package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteFavouriteRouteResponse {
    private Long id;
    private String label;
    private String fromLocation;
    private String toLocation;
    private Double fromLat;
    private Double fromLng;
    private Double toLat;
    private Double toLng;
    private LocalDateTime createdAt;
}
