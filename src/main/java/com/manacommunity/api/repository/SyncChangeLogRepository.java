package com.manacommunity.api.repository;

import com.manacommunity.api.model.SyncChangeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SyncChangeLogRepository extends JpaRepository<SyncChangeLog, Long> {

    @Query("SELECT s FROM SyncChangeLog s WHERE s.societyId = :societyId AND s.serverVectorTimestamp > :since ORDER BY s.serverVectorTimestamp ASC")
    List<SyncChangeLog> findChangesSince(@Param("societyId") Long societyId,
                                         @Param("since") Long sinceCheckpoint,
                                         Pageable pageable);

    @Query("SELECT COALESCE(MAX(s.serverVectorTimestamp), 0) FROM SyncChangeLog s WHERE s.societyId = :societyId")
    Long findMaxTimestamp(@Param("societyId") Long societyId);
}
