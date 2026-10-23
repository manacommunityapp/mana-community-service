package com.manacommunity.api.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncChangeLogRepository extends JpaRepository<SyncChangeLog, Long> {
    Optional<SyncChangeLog> findByClientMutationId(String clientMutationId);

    @Query("SELECT c FROM SyncChangeLog c WHERE c.community.id = :communityId AND c.id > :sinceChangeId ORDER BY c.id ASC")
    List<SyncChangeLog> findDeltaForCommunity(Long communityId, Long sinceChangeId);

    @Query("SELECT c FROM SyncChangeLog c WHERE c.user.id = :userId AND c.id > :sinceChangeId ORDER BY c.id ASC")
    List<SyncChangeLog> findDeltaForUser(Long userId, Long sinceChangeId);
}
