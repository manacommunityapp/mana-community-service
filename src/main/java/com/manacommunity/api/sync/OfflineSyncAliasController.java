package com.manacommunity.api.sync;

import com.manacommunity.api.sync.dto.SyncDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/api/v1/sync")
@RequiredArgsConstructor
public class OfflineSyncAliasController {

    private final OfflineSyncService offlineSyncService;

    @PostMapping("/push")
    public ResponseEntity<PushSyncBatchResult> push(@RequestBody PushSyncBatchRequest request) {
        return ResponseEntity.ok(offlineSyncService.processPushSync(request));
    }

    @PostMapping("/pull")
    public ResponseEntity<PullSyncResponse> pull(@RequestBody PullSyncRequest request) {
        return ResponseEntity.ok(offlineSyncService.processPullSync(request));
    }
}
