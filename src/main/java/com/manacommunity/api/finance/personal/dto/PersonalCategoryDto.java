package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalCategoryDto {
    private String id;
    private String name;
    private String icon;
    private String color;
    private String type; // INCOME, EXPENSE
    private List<PersonalSubcategoryDto> subcategories;
}
