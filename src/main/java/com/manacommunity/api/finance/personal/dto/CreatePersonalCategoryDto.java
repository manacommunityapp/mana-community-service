package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalCategoryDto {
    @NotBlank(message = "Category name is required")
    private String name;

    private String icon;
    private String color;

    @NotBlank(message = "Category type is required")
    @Pattern(regexp = "INCOME|EXPENSE", message = "Type must be INCOME or EXPENSE")
    private String type;

    private String parentId;
    private List<String> subcategories;
}
