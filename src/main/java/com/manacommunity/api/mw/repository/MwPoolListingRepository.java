package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwPoolListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MwPoolListingRepository extends JpaRepository<MwPoolListing, Long> {

    List<MwPoolListing> findByOrganizationIdOrderByShiftDateAsc(Long orgId);

    List<MwPoolListing> findByOrganizationIdAndStatusOrderByShiftDateAsc(Long orgId, String status);

    @Query("SELECT l FROM MwPoolListing l WHERE l.status = 'OPEN' AND l.shiftDate >= :today AND l.organizationId <> :excludeOrgId ORDER BY l.shiftDate ASC")
    List<MwPoolListing> findOpenListingsForBrowse(@Param("today") LocalDate today, @Param("excludeOrgId") Long excludeOrgId);

    @Query("SELECT l FROM MwPoolListing l WHERE l.status = 'OPEN' AND l.shiftDate >= :today ORDER BY l.shiftDate ASC")
    List<MwPoolListing> findAllOpenListings(@Param("today") LocalDate today);
}
