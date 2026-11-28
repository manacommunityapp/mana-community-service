package com.manacommunity.api.access.biometric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BiometricEdgeDeltaRepository extends JpaRepository<BiometricEdgeDelta, Long> {
    List<BiometricEdgeDelta> findByCommunityIdAndSyncVersionGreaterThanOrderBySyncVersionAsc(Long communityId, Long sinceVersion);

    @Query("SELECT COALESCE(MAX(d.syncVersion), 0) FROM BiometricEdgeDelta d WHERE d.communityId = :communityId")
    Long findMaxSyncVersionByCommunityId(@Param("communityId") Long communityId);
}
