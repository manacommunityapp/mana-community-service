package com.manacommunity.api.dto.email;

import com.manacommunity.api.model.EmailTheme;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailThemeDto {

    private Long id;
    private Long communityId;
    private String name;
    private String themeJson;
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EmailThemeDto from(EmailTheme theme) {
        return EmailThemeDto.builder()
                .id(theme.getId())
                .communityId(theme.getCommunity() != null ? theme.getCommunity().getId() : null)
                .name(theme.getName())
                .themeJson(theme.getThemeJson())
                .isDefault(theme.isDefault())
                .createdAt(theme.getCreatedAt())
                .updatedAt(theme.getUpdatedAt())
                .build();
    }
}
