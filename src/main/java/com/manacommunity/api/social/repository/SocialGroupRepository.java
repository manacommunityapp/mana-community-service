package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.SocialGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialGroupRepository extends JpaRepository<SocialGroup, Long> {
    Page<SocialGroup> findByCommunityIdAndIsActiveTrue(Long communityId, Pageable pageable);
    Page<SocialGroup> findByCommunityIdAndGroupTypeAndIsActiveTrue(Long communityId, String groupType, Pageable pageable);
    List<SocialGroup> findByCreatedByAndIsActiveTrue(Long createdBy);
}
