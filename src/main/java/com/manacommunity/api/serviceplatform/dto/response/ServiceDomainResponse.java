package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceDomainResponse {
    private Long id;
    private String name;
    private String slug;
    private String icon;
    private String description;
    private Integer displayOrder;
    private boolean active;
    private String metadata;
    private int categoryCount;
    private List<ServiceCategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
