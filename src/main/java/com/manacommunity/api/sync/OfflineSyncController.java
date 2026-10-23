package com.manacommunity.api.sync;

import com.manacommunity.api.sync.dto.SyncDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class OfflineSyncController {

    private final OfflineSyncService syncService;

    @PostMapping("/push")
    public ResponseEntity<PushSyncBatchResult> pushOfflineBatch(@Valid @RequestBody PushSyncBatchRequest request) {
        return ResponseEntity.ok(syncService.processPushSync(request));
    }

    @PostMapping("/pull")
    public ResponseEntity<PullSyncResponse> pullDelta(@Valid @RequestBody PullSyncRequest request) {
        return ResponseEntity.ok(syncService.processPullSync(request));
    }
}
