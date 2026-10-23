package com.manacommunity.api.noticeboard.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeStatsResponse {
    private Long noticeId;
    private long totalTargetUsers;
    private long totalReads;
    private double readPercentage;
    private long totalAcknowledgements;
    private double acknowledgementPercentage;
    private boolean requiresAcknowledgement;
}
