package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.GovernanceMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceMeetingRepository extends JpaRepository<GovernanceMeeting, Long> {
    List<GovernanceMeeting> findByCommunityIdOrderByMeetingDateDesc(Long communityId);
    List<GovernanceMeeting> findByCommunityIdAndTypeOrderByMeetingDateDesc(Long communityId, String type);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
