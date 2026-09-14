package com.manacommunity.api.dto.karate;

import com.manacommunity.api.model.karate.SportsKarateAttendance;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KarateAttendanceEntry {

    @NotNull
    private Long enrollmentId;

    @NotNull
    private SportsKarateAttendance.AttendanceStatus status;

    private String notes;
}
