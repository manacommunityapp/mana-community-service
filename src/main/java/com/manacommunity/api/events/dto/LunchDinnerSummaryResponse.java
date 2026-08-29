package com.manacommunity.api.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LunchDinnerSummaryResponse {
    private Long id;
    private Long communityId;
    private Long mainEventId;
    private String name;
    private String mealType;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;
    private Integer targetPlates;
    private String caterer;
    private String dietType;
    private BigDecimal fee;
    private Boolean isFree;
    private Boolean needsRegistration;
    private List<String> menuItems;
    private String notes;
    private Long bookedCount;
    private Long attendeeHeadcount;
}