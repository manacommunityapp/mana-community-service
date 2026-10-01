package com.manacommunity.api.sports.dto;

public record StandingResponse(
    int     position,
    Long    teamId,
    String  teamName,
    String  teamColor,
    int     played,
    int     won,
    int     lost,
    int     drawn,
    int     points,
    double  netRunRate,
    boolean qualified
) {}
