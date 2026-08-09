package com.manacommunity.api.dto;

import com.manacommunity.api.model.InvoiceCategory;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCategoryDto {

    private Long id;
    private Long communityId;
    private String name;
    private String code;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;

    public static InvoiceCategoryDto from(InvoiceCategory entity) {
        return InvoiceCategoryDto.builder()
                .id(entity.getId())
                .communityId(entity.getCommunityId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
