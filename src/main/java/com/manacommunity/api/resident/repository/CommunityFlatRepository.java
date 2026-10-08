package com.manacommunity.api.resident.repository;

import com.manacommunity.api.resident.model.CommunityFlat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityFlatRepository extends JpaRepository<CommunityFlat, Long> {

    Optional<CommunityFlat> findByCommunityIdAndTowerBlockIgnoreCaseAndFlatNumberIgnoreCase(
            Long communityId, String towerBlock, String flatNumber);

    List<CommunityFlat> findByCommunityId(Long communityId);

    @Query("SELECT cf FROM CommunityFlat cf WHERE cf.community.id = :communityId AND cf.verificationStatus = :status")
    List<CommunityFlat> findByCommunityIdAndVerificationStatus(
            @Param("communityId") Long communityId,
            @Param("status") String status);
}
