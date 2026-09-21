package com.manacommunity.api.service;

import com.manacommunity.api.dto.dashboard.SportsAnalyticsResponse;
import com.manacommunity.api.user.model.AppUser;

public interface SportsAnalyticsService {
    SportsAnalyticsResponse getAnalytics(AppUser user, Long communityId);
}
