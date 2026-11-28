package com.manacommunity.api.sync;

import com.manacommunity.api.sync.dto.SyncDtos.*;

import java.util.List;

public interface OfflineSyncService {
    PushSyncBatchResult processPushSync(PushSyncBatchRequest request);
    PullSyncResponse processPullSync(PullSyncRequest request);
    SignedGateRosterBundle registerGateDeviceAndRoster(Long communityId, GateDeviceRegisterRequest request);
    SignedGateRosterBundle getGateRoster(Long communityId, String deviceId);
    List<GateAnomalyDto> getUnresolvedGateAnomalies(Long communityId);
    GateAnomalyDto resolveGateAnomaly(Long communityId, Long auditId, Long resolverUserId, String note);
}
