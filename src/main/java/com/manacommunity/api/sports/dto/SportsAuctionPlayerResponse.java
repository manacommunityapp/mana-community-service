package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SportsAuctionPlayerResponse {
    private Long id;
    private Long configId;
    private Long userId;
    private String playerName;
    private String category;
    private String playerRole;
    private Integer age;
    private Integer basePrice;
    private String statsJson;
    private Integer queueOrder;
    private String status;
    private Long assignedTeamId;
    private String assignedTeamName;
    private Long soldPrice;
    private Boolean rtmUsed;
    private LocalDateTime soldAt;
    private Integer innings;
    private String bestBowling;
    private String cricHeroesId;
    private String cricHeroesUrl;
    private LocalDateTime verifiedAt;
}
