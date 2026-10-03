package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.user.model.AppUser;

import java.util.Map;

public interface SportsCricHeroesService {

    SportsCricHeroesLinkResponse linkProfile(SportsCricHeroesLinkRequest request, AppUser loggedInUser);

    void unlinkProfile(Long playerId, AppUser loggedInUser);

    SportsCricHeroesProfileResponse getProfile(Long playerId);

    SportsCricHeroesProfileResponse refreshProfile(Long playerId, AppUser loggedInUser);

    SportsCricHeroesProfileResponse previewProfile(String url);

    SportsPlayerRatingResponse getPlayerRating(Long playerId);

    Map<Long, SportsCricHeroesProfileResponse> getLinkedProfiles(Long configId);

    SportsTeamCompositionResponse getTeamComposition(Long configId, Long teamId);
}
