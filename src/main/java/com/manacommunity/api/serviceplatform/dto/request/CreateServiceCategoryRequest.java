package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceCategoryRequest {
    @NotNull
    private Long domainId;
    private Long parentCategoryId;
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 100)
    private String slug;
    @Size(max = 50)
    private String icon;
    private String description;
    private String requiredCertifications;
    private String customFields;
    private Integer displayOrder;
}
