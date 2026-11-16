package com.manacommunity.api.wellness.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseCatalogDto {
    private Long id;
    private String name;
    private String nameLocal;
    private String category;
    private String description;
    private BigDecimal metValue;
    private String musclesTargeted;
    private Boolean isEquipmentNeeded;
    private Boolean isBodyweight;
    private Boolean verified;
}
