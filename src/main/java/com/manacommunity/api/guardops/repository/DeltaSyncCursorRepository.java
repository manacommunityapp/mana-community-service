package com.manacommunity.api.guardops.repository;

import com.manacommunity.api.guardops.entity.DeltaSyncCursor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeltaSyncCursorRepository extends JpaRepository<DeltaSyncCursor, Long> {
    List<DeltaSyncCursor> findByDeviceIdOrderBySyncEntityAsc(String deviceId);
    List<DeltaSyncCursor> findByCommunityIdOrderByLastSyncAtDesc(String communityId);
}
