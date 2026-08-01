package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateServiceRequestDto {
    @NotNull
    private Long categoryId;
    @NotBlank @Size(max = 200)
    private String title;
    private String description;
    private LocalDate preferredDate;
    @Size(max = 50)
    private String preferredTimeSlot;
    private String address;
    private String urgency;
    private BigDecimal estimatedCost;
    private String customFieldValues;
    private String attachments;
    private boolean submitImmediately;
}
