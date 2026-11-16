package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.DeliveryBatchStatus;
import com.manacommunity.api.fresh.dispatch.model.HandlingClass;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class DeliveryBatchDto {
    private Long id;
    private String batchCode;
    private Long communityId;
    private String communityName;
    private LocalDate deliveryDate;
    private HandlingClass handlingClass;
    private Long assignedRunnerId;
    private String assignedRunnerName;
    private DeliveryBatchStatus status;
    private Instant dispatchedAt;
    private Instant completedAt;
    private List<DeliveryItemDto> items;
}