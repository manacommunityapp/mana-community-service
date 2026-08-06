package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceCategoryResponse {
    private Long id;
    private Long domainId;
    private String domainName;
    private Long parentCategoryId;
    private String parentCategoryName;
    private String name;
    private String slug;
    private String icon;
    private String description;
    private String requiredCertifications;
    private String customFields;
    private Integer displayOrder;
    private boolean active;
    private List<ServiceCategoryResponse> subCategories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
