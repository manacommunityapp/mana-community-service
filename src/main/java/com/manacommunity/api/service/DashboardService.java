package com.manacommunity.api.service;

import com.manacommunity.api.dto.dashboard.AdminDashboardStatsResponse;
import com.manacommunity.api.dto.dashboard.UserDashboardStatsResponse;
import com.manacommunity.api.user.model.AppUser;

public interface DashboardService {

    /**
     * Aggregates stats for the Admin Dashboard based on the caller's community context.
     */
    AdminDashboardStatsResponse getAdminStats(AppUser caller);

    /**
     * Aggregates personalized stats for the User Dashboard for the given logged-in user.
     */
    UserDashboardStatsResponse getUserStats(AppUser caller);
}
