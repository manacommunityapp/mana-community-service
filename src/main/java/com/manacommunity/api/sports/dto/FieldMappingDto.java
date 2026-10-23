package com.manacommunity.api.sports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingDto {
    private Long id;
    private String manaFieldKey;
    private String externalQuestionId;
    private String questionTitle;
    private String mappingType;
    private String mappingRulesJson;
    private Boolean required;
}
