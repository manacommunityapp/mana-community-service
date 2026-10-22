package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CposCrmActivityRepository extends JpaRepository<CposCrmActivity, Long> {
    List<CposCrmActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    @Query("SELECT a.callOutcome, COUNT(a) FROM CposCrmActivity a " +
           "WHERE a.lead.community.id = :communityId AND a.activityType = 'CALL' " +
           "AND a.callOutcome IS NOT NULL GROUP BY a.callOutcome")
    List<Object[]> countCallOutcomesByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT HOUR(a.createdAt), COUNT(a) FROM CposCrmActivity a " +
           "WHERE a.lead.community.id = :communityId GROUP BY HOUR(a.createdAt) ORDER BY HOUR(a.createdAt)")
    List<Object[]> countActivitiesByHour(@Param("communityId") Long communityId);
}
