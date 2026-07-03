package com.manacommunity.api.user.service;

import com.manacommunity.api.user.dto.UserProfileRequest;
import com.manacommunity.api.user.dto.UserProfileResponse;
import com.manacommunity.api.user.model.AppUser;

public interface UserProfileService {
    UserProfileResponse getProfile(AppUser user);
    UserProfileResponse updateProfile(AppUser user, UserProfileRequest request);
}
