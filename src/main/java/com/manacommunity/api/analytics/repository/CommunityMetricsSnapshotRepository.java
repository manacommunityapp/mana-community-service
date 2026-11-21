package com.manacommunity.api.analytics.repository;

import com.manacommunity.api.analytics.model.CommunityMetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CommunityMetricsSnapshotRepository extends JpaRepository<CommunityMetricsSnapshot, Long> {

    Optional<CommunityMetricsSnapshot> findByCommunityIdAndSnapshotDate(Long communityId, LocalDate date);

    @Query("SELECT s FROM CommunityMetricsSnapshot s WHERE s.communityId = :cid AND s.snapshotDate >= :from ORDER BY s.snapshotDate ASC")
    List<CommunityMetricsSnapshot> findRecentByCommunityId(@Param("cid") Long communityId, @Param("from") LocalDate from);
}
