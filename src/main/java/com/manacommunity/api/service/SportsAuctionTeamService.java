package com.manacommunity.api.service;

import com.manacommunity.api.dto.SportsAuctionTeamRequest;
import com.manacommunity.api.model.SportsAuctionTeam;
import java.util.List;

public interface SportsAuctionTeamService {
    List<SportsAuctionTeam> getTeams(Long configId);
    List<SportsAuctionTeam> getNominatedCaptains(Long eventId);
    SportsAuctionTeam createTeam(SportsAuctionTeamRequest req, Long adminUserId);
    SportsAuctionTeam confirmCaptain(Long teamId, boolean confirm, Long callerUserId, boolean isAdmin);
    SportsAuctionTeam nominateCaptain(Long eventId, Long userId, boolean nominate, String teamName);
    List<SportsAuctionTeam> getMyNominations(Long userId);

    List<SportsAuctionTeam> getCaptainRegistration(Long id);
}
