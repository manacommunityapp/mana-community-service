package com.manacommunity.api.service;

import com.manacommunity.api.dto.EventVenueConfigDto;
import com.manacommunity.api.model.EventVenueConfig;
import com.manacommunity.api.repository.EventVenueConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventVenueConfigService {

    private final EventVenueConfigRepository repository;

    @Transactional(readOnly = true)
    public EventVenueConfigDto getVenueConfig(Long eventId, Long communityId) {
        Optional<EventVenueConfig> configOpt = Optional.empty();
        if (eventId != null) {
            configOpt = repository.findTopByEventIdOrderByUpdatedAtDesc(eventId);
        }
        if (configOpt.isEmpty() && communityId != null) {
            configOpt = repository.findTopByCommunityIdOrderByUpdatedAtDesc(communityId);
        }
        if (configOpt.isEmpty()) {
            configOpt = repository.findTopByOrderByUpdatedAtDesc();
        }

        return configOpt.map(EventVenueConfigDto::from).orElse(null);
    }

    @Transactional
    public EventVenueConfigDto saveVenueConfig(EventVenueConfigDto dto) {
        EventVenueConfig config = null;

        if (dto.getId() != null) {
            config = repository.findById(dto.getId()).orElse(null);
        }
        if (config == null && dto.getEventId() != null) {
            config = repository.findTopByEventIdOrderByUpdatedAtDesc(dto.getEventId()).orElse(null);
        }

        if (config == null) {
            config = EventVenueConfig.builder()
                    .eventId(dto.getEventId())
                    .communityId(dto.getCommunityId())
                    .venueName(dto.getVenueName() != null ? dto.getVenueName() : "Main Community Grounds")
                    .zonesJson(dto.getZonesJson())
                    .facilitiesJson(dto.getFacilitiesJson())
                    .build();
        } else {
            if (dto.getVenueName() != null) config.setVenueName(dto.getVenueName());
            if (dto.getZonesJson() != null) config.setZonesJson(dto.getZonesJson());
            if (dto.getFacilitiesJson() != null) config.setFacilitiesJson(dto.getFacilitiesJson());
            if (dto.getCommunityId() != null) config.setCommunityId(dto.getCommunityId());
        }

        EventVenueConfig saved = repository.save(config);
        log.info("Event venue configuration saved for id={}", saved.getId());
        return EventVenueConfigDto.from(saved);
    }
}
