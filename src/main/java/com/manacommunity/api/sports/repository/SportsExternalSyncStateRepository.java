package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsExternalSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportsExternalSyncStateRepository extends JpaRepository<SportsExternalSyncState, Long> {

    Optional<SportsExternalSyncState> findByFormId(Long formId);

    Optional<SportsExternalSyncState> findByWatchId(String watchId);
}
