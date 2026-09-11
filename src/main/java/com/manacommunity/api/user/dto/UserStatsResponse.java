package com.manacommunity.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long pendingKyc;
    private long approvedKyc;
    private long rejectedKyc;
    private Map<String, Long> roleBreakdown;
}
