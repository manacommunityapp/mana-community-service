package com.manacommunity.api.wellness.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGoalDto {
    private Long id;
    private Long userId;
    private Integer dailyStepsTarget;
    private Integer activeMinutesTarget;
    private Integer weeklyWorkoutsTarget;
}
