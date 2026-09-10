package com.manacommunity.api.marketplace.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAuditLogDto {

    private Long id;
    private String action;
    private String targetEntity;
    private Long targetId;
    private Long actorId;
    private String actorName;
    private Long communityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
