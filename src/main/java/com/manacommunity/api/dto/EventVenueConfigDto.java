package com.manacommunity.api.dto;

import com.manacommunity.api.model.EventVenueConfig;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventVenueConfigDto {

    private Long id;
    private Long eventId;
    private Long communityId;
    private String venueName;
    private String zonesJson;
    private String facilitiesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventVenueConfigDto from(EventVenueConfig entity) {
        if (entity == null) return null;
        return EventVenueConfigDto.builder()
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
