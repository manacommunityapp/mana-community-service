package com.manacommunity.api.safety.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrolSessionResponse {
    private Long sessionId;
    private Long guardId;
    private String guardName;
    private String startedAt;
    private String endedAt;
    private String status;
    private int checkpointsScanned;
    private int totalCheckpoints;
}
