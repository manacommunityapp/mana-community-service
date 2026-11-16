package com.manacommunity.api.security.camera;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SnapshotResponse {
    private Long cameraId;
    private String snapshotUrl;
    private LocalDateTime takenAt;
}
