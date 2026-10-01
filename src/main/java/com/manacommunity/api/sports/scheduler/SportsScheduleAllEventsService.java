package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import com.manacommunity.api.sports.dto.SportsScheduleStatsResponse;
import com.manacommunity.api.user.model.AppUser;

public interface SportsScheduleAllEventsService {
    SportsScheduleStatsResponse getStats(AppUser user);
    long getTotalGames(AppUser user);
    long getLiveGames(AppUser user);
    long getUpcomingGames(AppUser user);
    long getCompletedGames(AppUser user);
}
