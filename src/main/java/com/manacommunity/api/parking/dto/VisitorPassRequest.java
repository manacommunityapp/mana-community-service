package com.manacommunity.api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorPassRequest {

    @NotBlank(message = "Visitor name is required")
    private String visitorName;

    private String visitorPhone;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    private String vehicleType;

    private Long spotId;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private String purpose;
}
