package com.manacommunity.api.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleServiceSlotResponse {
    private Long id;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int totalCapacity;
    private int bookedCount;
    private int remaining;
    private boolean available;
}
