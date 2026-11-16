package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityMeetingRepository extends JpaRepository<CommunityMeeting, Long> {
    List<CommunityMeeting> findByCommunityIdOrderByMeetingDateDesc(Long communityId);
    List<CommunityMeeting> findByCommunityIdAndTypeOrderByMeetingDateDesc(Long communityId, String type);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
