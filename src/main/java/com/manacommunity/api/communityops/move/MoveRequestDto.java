package com.manacommunity.api.communityops.move;

import java.time.LocalDate;
import java.time.LocalTime;

public record MoveRequestDto(
    String flatNumber,
    MoveType moveType,
    LocalDate plannedDate,
    LocalTime plannedTime,
    String vehicleNumber,
    String moversCompany,
    String contactNumber
) {}
