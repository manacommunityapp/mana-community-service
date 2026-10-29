package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipantDto {
    private Long id;
    private Long userId;
    private String userName;
    private Integer currentProgress;
    private LocalDate joinedDate;
    private Boolean completed;
    private LocalDate completedDate;
    private Boolean badgeEarned;
}
