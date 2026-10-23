package com.manacommunity.api.sync;

import com.manacommunity.api.sync.dto.SyncDtos.*;

public interface OfflineSyncService {
    PushSyncBatchResult processPushSync(PushSyncBatchRequest request);
    PullSyncResponse processPullSync(PullSyncRequest request);
}
