package com.manacommunity.api.security.camera;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SecurityCameraResponse {
    private Long id;
    private String name;
    private String location;
    private String type;
    private String status;
    private String streamUrl;
    private String snapshotUrl;
    private LocalDateTime lastSnapshotAt;
    private LocalDate installDate;
    private String ipAddress;
}
