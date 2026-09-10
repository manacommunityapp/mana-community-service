package com.manacommunity.api.dto;

public record SportsAuctionConfigResponse(
    Long   id,
    Long   eventId,
    String eventName,
    String sportName,
    String seasonName,
    String auctionFormat,
    int    totalTeams,
    int    totalPlayers,
    long   budgetPerTeam,
    int    basePrice,
    int    bidIncrementDefault,
    long   bidIncrementThreshold,
    int    bidIncrementAbove,
    int    bidTimerSeconds,
    boolean rtmEnabled,
    String  unsoldRule,
    String  status,
    java.util.List<String> categories,
    java.util.List<String> committeeMembers
) {}
