package com.manacommunity.api.noticeboard.statutory.repository;

import com.manacommunity.api.noticeboard.statutory.entity.AudienceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudienceGroupRepository extends JpaRepository<AudienceGroup, Long> {
    List<AudienceGroup> findByCommunityIdAndGroupStatus(String communityId, AudienceGroup.GroupStatus status);
    List<AudienceGroup> findByDynamicTrue();
}
