package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.model.StaffJobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffJobPostRepository extends JpaRepository<StaffJobPost, Long> {

    List<StaffJobPost> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<StaffJobPost> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, StaffJobPost.JobPostStatus status);
}
