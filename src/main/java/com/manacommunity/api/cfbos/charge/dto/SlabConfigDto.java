package com.manacommunity.api.cfbos.charge.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlabConfigDto {
    private Long id;
    private String name;
    private String description;
    private String unitLabel;
    private Boolean isActive;
    private List<TierDto> tiers;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TierDto {
        private BigDecimal tierFrom;
        private BigDecimal tierTo;
        private BigDecimal rate;
        private BigDecimal fixedCharge;
        private Integer tierOrder;
    }
}
