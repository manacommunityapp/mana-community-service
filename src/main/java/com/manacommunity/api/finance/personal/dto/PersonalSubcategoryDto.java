package com.manacommunity.api.finance.personal.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalSubcategoryDto {
    private String id;
    private String name;
    private String icon;
    private String parentId;
}
