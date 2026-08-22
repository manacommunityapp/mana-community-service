package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityWhoToCallHistoryResponse {

    private Long id;
    private Long whoToCallId;
    private Long communityId;
    private String action;
    private Long changedByUserId;
    private String changedByName;
    private String department;
    private String contactPerson;
    private String phoneNumber;
    private String changeSummary;
    private String snapshotData;
    private LocalDateTime createdAt;
}
