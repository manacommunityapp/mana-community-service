package com.manacommunity.api.db.optimization.mysql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lightweight visitor projection optimized for covering index scans.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorCursorRecord {
    private Long id;
    private Long communityId;
    private String passCode;
    private String visitorName;
    private String vehicleNumber;
    private String status;
    private Instant createdAt;
}
