package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SportsCricHeroesLinkRequest {

    @NotNull
    private Long playerId;

    @NotBlank
    private String cricHeroesUrl;

    private String formatScope;
}
