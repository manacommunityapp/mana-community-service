package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.dto.SportsScheduleStatsResponse;
import com.manacommunity.api.user.model.AppUser;

public interface SportsScheduleAllEventsService {
    SportsScheduleStatsResponse getStats(AppUser user);
    long getTotalGames(AppUser user);
    long getLiveGames(AppUser user);
    long getUpcomingGames(AppUser user);
    long getCompletedGames(AppUser user);
}
