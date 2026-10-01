package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.dto.SportsMatchResponse;


import java.util.List;

public record RoundResponse(
    String              roundName,
    int                 roundNumber,
    List<SportsMatchResponse> matches
) {}
