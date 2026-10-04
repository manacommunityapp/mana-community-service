package com.manacommunity.api.serviceplatform.amc.dto;

import com.manacommunity.api.serviceplatform.amc.entity.WarrantyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWarrantyDto {
    private Long id;
    private Long workOrderId;
    private int warrantyPeriodDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private String terms;
    private WarrantyStatus status;
}
