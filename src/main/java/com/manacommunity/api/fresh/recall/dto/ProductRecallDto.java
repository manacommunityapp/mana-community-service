package com.manacommunity.api.fresh.recall.dto;

import com.manacommunity.api.fresh.recall.model.RecallSeverity;
import com.manacommunity.api.fresh.recall.model.RecallStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecallDto {
    private Long id;
    private String recallReference;
    private Long batchId;
    private String batchNumber;
    private Long vendorProductId;
    private String productName;
    private RecallSeverity severity;
    private String reason;
    private String detailedRootCause;
    private Long initiatedByUserId;
    private String initiatedByUserName;
    private Instant initiatedAt;
    private RecallStatus status;
    private Integer affectedOrdersCount;
    private Integer affectedCustomersCount;
    private BigDecimal totalUnitsRecalled;
    private BigDecimal totalRefundAmount;
    private List<RecallOrderDto> affectedOrders;
    private Instant createdAt;
}
