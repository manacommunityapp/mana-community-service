package com.manacommunity.api.groupbuying.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyingGroupDto {
    private Long id;
    private Long communityId;
    private String name;
    private String tower;
    private String block;
    private String leaderName;
    private String leaderFlat;
    private String description;
    private BigDecimal totalSaved;
    private Integer memberCount;
    private Integer activeDealsCount;
    private Boolean isMember;
}
