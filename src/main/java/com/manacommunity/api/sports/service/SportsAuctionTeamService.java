package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsAuctionTeamRequest;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import java.util.List;

public interface SportsAuctionTeamService {
    List<SportsAuctionTeam> getTeams(Long configId);
    List<SportsAuctionTeam> getNominatedCaptains(Long eventId);
    SportsAuctionTeam createTeam(SportsAuctionTeamRequest req, Long adminUserId);
    SportsAuctionTeam confirmCaptain(Long teamId, boolean confirm, Long callerUserId, boolean isAdmin);
    default SportsAuctionTeam nominateCaptain(Long eventId, Long userId, boolean nominate, String teamName) {
        return nominateCaptain(eventId, userId, false, nominate, teamName);
    }
    SportsAuctionTeam nominateCaptain(Long eventId, Long userId, boolean isAdmin, boolean nominate, String teamName);
    List<SportsAuctionTeam> getMyNominations(Long userId);

    List<SportsAuctionTeam> getCaptainRegistration(Long id);
    void leaveTeam(Long teamId, Long userId);
    void dissolveTeam(Long teamId, Long ownerUserId);
}
