package com.manacommunity.api.fresh.catalog.dto;

import com.manacommunity.api.fresh.catalog.model.FreshProductAllergen.Severity;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class FreshProductAllergenDto {
    private Long id;
    private String allergenName;
    private Severity severity;
}