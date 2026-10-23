package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalBillDto {
    private String id;
    private String name;
    private BigDecimal amount;
    private String dueDate;
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private boolean isPaid;
    private int reminderDaysBefore;
}
