package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceCorrectionRequest {
    private LocalTime correctedCheckIn;
    private LocalTime correctedCheckOut;
    private String correctedStatus;
    @NotBlank(message = "Reason is required for attendance correction")
    private String reason;
}
