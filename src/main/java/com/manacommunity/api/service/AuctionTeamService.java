package com.manacommunity.api.service;

import com.manacommunity.api.dto.AuctionTeamRequest;
import com.manacommunity.api.model.AuctionTeam;
import java.util.List;

public interface AuctionTeamService {
    List<AuctionTeam> getTeams(Long configId);
    List<AuctionTeam> getNominatedCaptains(Long eventId);
    AuctionTeam createTeam(AuctionTeamRequest req, Long adminUserId);
    AuctionTeam confirmCaptain(Long teamId, boolean confirm, Long callerUserId, boolean isAdmin);
    AuctionTeam nominateCaptain(Long eventId, Long userId, boolean nominate, String teamName);
    List<AuctionTeam> getMyNominations(Long userId);

    List<AuctionTeam> getCaptainRegistration(Long id);
}
