package com.manacommunity.api.sports.repository;
import com.manacommunity.api.sports.controller.SportsController;
import com.manacommunity.api.sports.service.impl.SportsEventServiceImpl;


import com.manacommunity.api.sports.model.SportsPlayerCategory;
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

    /**
     * Check if a category with the same name exists within the relevant scope (DEFAULT or community),
     * optionally excluding an existing category ID during updates.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM SportsPlayerCategory c " +
           "WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name)) " +
           "AND (" +
           "  (:communityId IS NULL AND (c.type = 'DEFAULT' OR c.community IS NULL)) " +
           "  OR (:communityId IS NOT NULL AND (c.type = 'DEFAULT' OR c.community.id = :communityId))" +
           ") " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsDuplicateName(@Param("name") String name,
                                @Param("communityId") Long communityId,
                                @Param("excludeId") Long excludeId);
}


