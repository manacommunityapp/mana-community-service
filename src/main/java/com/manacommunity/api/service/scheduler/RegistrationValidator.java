package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.dto.scheduler.TournamentConfigRequest;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.repository.AuctionTeamRepository;
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
public class RegistrationValidator {

    private final AuctionTeamRepository teamRepo;

    /**
     * Resolves the requested team IDs and verifies the count matches
     * {@code totalTeams}. Throws {@link IllegalArgumentException} on mismatch.
     */
    public List<AuctionTeam> validateTeams(TournamentConfigRequest req) {
        List<AuctionTeam> teams = teamRepo.findAllById(req.teamIds());
        if (teams.size() != req.totalTeams()) {
            throw new InvalidInputException(
                "Expected " + req.totalTeams() + " teams, got " + teams.size());
        }
        return teams;
    }
}
