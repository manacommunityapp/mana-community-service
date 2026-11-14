package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SportsMatchTossRequest(
    @NotNull Long tossWinnerTeamId,
    @NotBlank @Pattern(regexp = "BAT|BOWL", flags = Pattern.Flag.CASE_INSENSITIVE) String tossDecision
) {}
