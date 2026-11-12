package com.manacommunity.api.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SetupMandateRequest {
    private String bankName;
    private String accountNumber;
    private String ifsc;
    private String accountHolderName;
    private BigDecimal monthlyAmount;
}
