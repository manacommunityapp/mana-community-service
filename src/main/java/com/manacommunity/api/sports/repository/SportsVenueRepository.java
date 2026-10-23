package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsVenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsVenueRepository extends JpaRepository<SportsVenue, Long> {
    @Query("SELECT v FROM SportsVenue v LEFT JOIN v.community c WHERE c.id = :communityId OR c.id IS NULL")
    List<SportsVenue> findByCommunityIdOrCommunityIdIsNull(@Param("communityId") Long communityId);
}
