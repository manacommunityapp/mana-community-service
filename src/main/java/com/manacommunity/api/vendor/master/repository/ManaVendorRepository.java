package com.manacommunity.api.vendor.master.repository;

import com.manacommunity.api.vendor.master.entity.ManaVendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManaVendorRepository extends JpaRepository<ManaVendor, Long> {

    List<ManaVendor> findByCommunityIdAndStatusOrderByNameAsc(Long communityId, String status);

    List<ManaVendor> findByCommunityIdOrderByNameAsc(Long communityId);

    @Query("SELECT v FROM ManaVendor v WHERE v.communityId = :communityId " +
           "AND (LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(v.email) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "AND v.status = 'ACTIVE' " +
           "ORDER BY v.name ASC")
    List<ManaVendor> searchActive(@Param("communityId") Long communityId, @Param("q") String q);

    Page<ManaVendor> findByCommunityId(Long communityId, Pageable pageable);

    Optional<ManaVendor> findByIdAndCommunityId(Long id, Long communityId);

    boolean existsByGstinAndCommunityId(String gstin, Long communityId);

    boolean existsByGstinAndCommunityIdAndIdNot(String gstin, Long communityId, Long id);

    long countByCommunityId(Long communityId);
}
