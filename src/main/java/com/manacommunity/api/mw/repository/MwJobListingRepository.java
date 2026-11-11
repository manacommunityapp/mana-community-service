package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwJobListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MwJobListingRepository extends JpaRepository<MwJobListing, Long> {

    List<MwJobListing> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);

    @Query("SELECT j FROM MwJobListing j WHERE j.status = 'ACTIVE' AND (j.expiresAt IS NULL OR j.expiresAt >= :today) ORDER BY j.createdAt DESC")
    List<MwJobListing> findActiveCommunityListings(@Param("today") LocalDate today);

    @Query("SELECT j FROM MwJobListing j WHERE j.status = 'ACTIVE' AND (j.expiresAt IS NULL OR j.expiresAt >= :today) AND LOWER(j.title) LIKE %:search% ORDER BY j.createdAt DESC")
    List<MwJobListing> searchActiveCommunityListings(@Param("today") LocalDate today, @Param("search") String search);
}
