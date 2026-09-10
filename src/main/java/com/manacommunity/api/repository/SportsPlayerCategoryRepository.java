package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsPlayerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BUG FIX: Repository was missing entirely. SportsEventServiceImpl and
 * SportsController both inject SportsPlayerCategoryRepository but no such
 * interface existed, causing a Spring context startup failure.
 */
@Repository
public interface SportsPlayerCategoryRepository extends JpaRepository<SportsPlayerCategory, Long> {

    /** Find categories belonging to a specific community */
    List<SportsPlayerCategory> findByCommunityId(Long communityId);

    java.util.Optional<SportsPlayerCategory> findByNameIgnoreCase(String name);

    /**
     * For non-super-admin users: returns DEFAULT type categories (visible to everyone)
     * PLUS categories belonging to the user's community.
     */
    @Query("SELECT c FROM SportsPlayerCategory c WHERE c.type = 'DEFAULT' OR c.community.id = :communityId")
    List<SportsPlayerCategory> findDefaultAndCommunityCategories(@Param("communityId") Long communityId);
}

