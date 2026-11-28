package com.manacommunity.api.pins.dto;

import com.manacommunity.api.pins.entity.CommunityPin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPinDto {

    private Long id;
    private Long communityId;
    private Long authorId;
    private String authorName;
    private String flatNumber;
    private String category;
    private String content;
    private String imageUrl;
    private String externalLink;
    private Instant expiresAt;
    private Boolean isClosed;
    private Instant closedAt;
    private Integer likesCount;
    private Instant createdAt;
    private Boolean isExpired;

    public static CommunityPinDto fromEntity(CommunityPin pin) {
        if (pin == null) return null;
        boolean expired = pin.getExpiresAt() != null && pin.getExpiresAt().isBefore(Instant.now());
        return CommunityPinDto.builder()
                .id(pin.getId())
                .communityId(pin.getCommunity() != null ? pin.getCommunity().getId() : null)
                .authorId(pin.getAuthor() != null ? pin.getAuthor().getId() : null)
                .authorName(pin.getAuthorName())
                .flatNumber(pin.getFlatNumber())
                .category(pin.getCategory())
                .content(pin.getContent())
                .imageUrl(pin.getImageUrl())
                .externalLink(pin.getExternalLink())
                .expiresAt(pin.getExpiresAt())
                .isClosed(Boolean.TRUE.equals(pin.getIsClosed()))
                .closedAt(pin.getClosedAt())
                .likesCount(pin.getLikesCount() != null ? pin.getLikesCount() : 0)
                .createdAt(pin.getCreatedAt())
                .isExpired(expired)
                .build();
    }
}
