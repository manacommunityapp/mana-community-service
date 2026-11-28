package com.manacommunity.api.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GateEntryAuditRepository extends JpaRepository<GateEntryAudit, Long> {
    boolean existsByCommunityIdAndPassNonce(Long communityId, String passNonce);
    Optional<GateEntryAudit> findByDeviceIdAndClientMutationId(String deviceId, String clientMutationId);
    List<GateEntryAudit> findTop100ByCommunityIdOrderByReconciledAtDesc(Long communityId);
    List<GateEntryAudit> findByCommunityIdAndResolvedFalseAndSyncStatusInOrderByReconciledAtDesc(
            Long communityId, List<SyncEnums.SyncStatus> statuses);
}
