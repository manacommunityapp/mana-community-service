package com.manacommunity.api.cfbos.treasury.dto;

import com.manacommunity.api.cfbos.treasury.enums.FundType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundAccountDto {
    private Long id;
    private String name;
    private FundType fundType;
    private BigDecimal currentBalance;
    private BigDecimal targetAmount;
    private BigDecimal minimumReserveFloor;
    private String bankAccountNumber;
    private String bankName;
    private String description;
    private Boolean active;
}
