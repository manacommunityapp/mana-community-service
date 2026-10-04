package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SportsCricHeroesLinkResponse {
    private Long playerId;
    private String cricheroesId;
    private String resolvedUrl;
    private SportsCricHeroesProfileResponse profile;
    private LocalDateTime linkedAt;
}
