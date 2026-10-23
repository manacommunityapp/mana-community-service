package com.manacommunity.api.homeservices.service.impl;

import com.manacommunity.api.homeservices.dto.StaffJobPostRequest;
import com.manacommunity.api.homeservices.dto.StaffJobPostResponse;
import com.manacommunity.api.homeservices.model.StaffJobPost;
import com.manacommunity.api.homeservices.repository.StaffJobPostRepository;
import com.manacommunity.api.homeservices.service.StaffJobPostService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffJobPostServiceImpl implements StaffJobPostService {

    private final StaffJobPostRepository jobPostRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StaffJobPostResponse> getJobPosts(Long communityId) {
        return jobPostRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StaffJobPostResponse createJobPost(StaffJobPostRequest request, AppUser user) {
        Community community = user.getCommunity();

        StaffJobPost jobPost = StaffJobPost.builder()
                .community(community)
                .postedBy(user)
                .title(request.getTitle())
                .role(request.getRole())
                .description(request.getDescription())
                .salaryRange(request.getSalaryRange())
                .shiftPreference(request.getShiftPreference())
                .requirements(request.getRequirements() != null ? request.getRequirements() : List.of())
                .build();

        return toResponse(jobPostRepository.save(jobPost));
    }

    @Override
    @Transactional
    public StaffJobPostResponse closeJobPost(Long id) {
        StaffJobPost jobPost = jobPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job post not found with id: " + id));

        jobPost.setStatus(StaffJobPost.JobPostStatus.CLOSED);
        return toResponse(jobPostRepository.save(jobPost));
    }

    private StaffJobPostResponse toResponse(StaffJobPost jobPost) {
        StaffJobPostResponse response = new StaffJobPostResponse();
        response.setId(jobPost.getId());
        response.setTitle(jobPost.getTitle());
        response.setRole(jobPost.getRole().name());
        response.setDescription(jobPost.getDescription());
        response.setSalaryRange(jobPost.getSalaryRange());
        response.setShiftPreference(jobPost.getShiftPreference());
        response.setRequirements(jobPost.getRequirements());
        response.setStatus(jobPost.getStatus().name());
        response.setApplicantCount(jobPost.getApplicantCount());
        response.setPostedByName(jobPost.getPostedBy() != null ? jobPost.getPostedBy().getFullName() : null);
        response.setCreatedAt(jobPost.getCreatedAt());
        return response;
    }
}
