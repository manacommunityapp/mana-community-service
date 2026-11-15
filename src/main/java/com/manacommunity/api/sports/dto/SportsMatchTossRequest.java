package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// tossDecision covers cricket (BAT/BOWL) and all other sports:
// SERVE/RECEIVE (badminton, tennis, volleyball, carrom),
// KICKOFF/DEFEND (football, hockey, rugby, futsal),
// RAID/DEFEND (kabaddi), GO_FIRST/GO_SECOND (generic)
public record SportsMatchTossRequest(
    @NotNull Long tossWinnerTeamId,
    @NotBlank @Size(max = 20)
    @Pattern(
        regexp = "BAT|BOWL|SERVE|RECEIVE|KICKOFF|DEFEND|RAID|GO_FIRST|GO_SECOND",
        flags = Pattern.Flag.CASE_INSENSITIVE
    ) String tossDecision
) {}
