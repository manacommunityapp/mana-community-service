package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsAnalyticsResponse;
import com.manacommunity.api.user.model.AppUser;

public interface SportsAnalyticsService {
    SportsAnalyticsResponse getAnalytics(AppUser user, Long communityId);
}
