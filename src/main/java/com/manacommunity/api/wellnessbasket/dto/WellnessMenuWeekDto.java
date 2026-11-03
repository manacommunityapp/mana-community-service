package com.manacommunity.api.wellnessbasket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessMenuWeekDto {
    private Short cycleWeek;
    private LocalDate weekStartDate; // Monday
    private LocalDate weekEndDate;   // Saturday
    private String planType;
    private List<WellnessMenuDayDto> days;
    private boolean isSwapWindowOpen;
    private String swapWindowCloseMessage;
}
