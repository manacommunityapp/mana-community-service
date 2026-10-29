package com.manacommunity.api.wellness.dto;

import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessChallengeDto {
    private Long id;
    private String title;
    private String description;
    private ChallengeType challengeType;
    private ChallengeStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer targetValue;
    private String targetUnit;
    private Integer maxParticipants;
    private Long communityId;
    private Long createdByUserId;
    private String badgeName;
    private String badgeIcon;
    private Integer participantCount;
    private List<ChallengeParticipantDto> participants;
}
