package com.manacommunity.api.user.service;

import com.manacommunity.api.user.dto.UserProfileRequest;
import com.manacommunity.api.user.dto.UserProfileResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface UserProfileService {
    UserProfileResponse getProfile(AppUser user);
    UserProfileResponse.UserStats getProfileStats(AppUser user);
    UserProfileResponse updateProfile(AppUser user, UserProfileRequest request);
    List<UserProfileResponse.UserActivityItem> getUserActivities(AppUser user);
}
