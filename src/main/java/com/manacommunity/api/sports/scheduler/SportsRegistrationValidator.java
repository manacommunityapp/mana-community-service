package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import com.manacommunity.api.sports.dto.SportsTournamentConfigRequest;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.SportsAuctionTeamRepository;
import com.manacommunity.api.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validates the teams/registrations a tournament will be generated from before
 * any schedule is built.
 */
@Service
@RequiredArgsConstructor
public class SportsRegistrationValidator {

    private final SportsAuctionTeamRepository teamRepo;

    /**
     * Resolves the requested team IDs and verifies the count matches
     * {@code totalTeams}. Throws {@link IllegalArgumentException} on mismatch.
     */
    public List<SportsAuctionTeam> validateTeams(SportsTournamentConfigRequest req) {
        List<SportsAuctionTeam> teams = teamRepo.findAllById(req.teamIds());
        if (teams.size() != req.totalTeams()) {
            throw new InvalidInputException(
                "Expected " + req.totalTeams() + " teams, got " + teams.size());
        }
        return teams;
    }
}
