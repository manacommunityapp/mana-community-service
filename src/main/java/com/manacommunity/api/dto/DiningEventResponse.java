package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningEventResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalTime time;
    private String venue;
    private Integer maxCapacity;
    private Integer currentRsvps;
    private BigDecimal pricePerPerson;
    private String menuDescription;
    private Long hostUserId;
    private String hostUserName;
    private String status;
    private LocalDateTime createdAt;
}
