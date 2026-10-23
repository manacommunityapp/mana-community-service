package com.manacommunity.api.sports.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDTO {
    private String title;
    private String imageUrl;
    private String bgColor;
    private String icon;
}
