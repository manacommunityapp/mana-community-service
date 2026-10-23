package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.dto.StaffJobPostRequest;
import com.manacommunity.api.homeservices.dto.StaffJobPostResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface StaffJobPostService {

    List<StaffJobPostResponse> getJobPosts(Long communityId);

    StaffJobPostResponse createJobPost(StaffJobPostRequest request, AppUser user);

    StaffJobPostResponse closeJobPost(Long id);
}
