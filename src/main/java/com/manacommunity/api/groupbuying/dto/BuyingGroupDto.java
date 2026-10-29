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
    private String towerName;
    private String block;
    private String leaderName;
    private String leaderFlat;
    private String description;
    private BigDecimal totalSaved;
    private BigDecimal totalSavingsAmount;
    private Integer memberCount;
    private Integer membersCount;
    private Integer activeDealsCount;
    private String topCategory;
    private Boolean isMember;
    private Boolean isJoined;
}
