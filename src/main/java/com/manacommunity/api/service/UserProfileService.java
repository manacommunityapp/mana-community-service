package com.manacommunity.api.service;

import com.manacommunity.api.dto.UserProfileRequest;
import com.manacommunity.api.dto.UserProfileResponse;
import com.manacommunity.api.model.AppUser;

public interface UserProfileService {
    UserProfileResponse getProfile(AppUser user);
    UserProfileResponse updateProfile(AppUser user, UserProfileRequest request);
}
