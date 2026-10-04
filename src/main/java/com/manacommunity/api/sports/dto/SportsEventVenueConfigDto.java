package com.manacommunity.api.sports.dto;

import com.manacommunity.api.sports.model.SportsEventVenueConfig;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsEventVenueConfigDto {

    private Long id;
    private Long eventId;
    private Long communityId;
    private String venueName;
    private String zonesJson;
    private String facilitiesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SportsEventVenueConfigDto from(SportsEventVenueConfig entity) {
        if (entity == null) return null;
        return SportsEventVenueConfigDto.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .communityId(entity.getCommunityId())
                .venueName(entity.getVenueName())
                .zonesJson(entity.getZonesJson())
                .facilitiesJson(entity.getFacilitiesJson())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
