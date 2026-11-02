package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsAnalyticsResponse;
import com.manacommunity.api.user.model.AppUser;

import java.time.LocalDate;

public interface SportsAnalyticsService {
    SportsAnalyticsResponse getAnalytics(AppUser user, Long communityId);

    SportsAnalyticsResponse getAnalytics(AppUser user, Long communityId, Long sportId, LocalDate fromDate, LocalDate toDate);
}
