package com.manacommunity.api.controller;

import com.manacommunity.api.dto.EventVenueConfigDto;
import com.manacommunity.api.service.EventVenueConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/venues/config")
@RequiredArgsConstructor
public class EventVenueConfigController {

    private final EventVenueConfigService venueConfigService;

    @GetMapping
    public ResponseEntity<EventVenueConfigDto> getVenueConfig(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long communityId) {
        EventVenueConfigDto dto = venueConfigService.getVenueConfig(eventId, communityId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<EventVenueConfigDto> saveVenueConfig(
            @RequestBody EventVenueConfigDto dto) {
        EventVenueConfigDto saved = venueConfigService.saveVenueConfig(dto);
        return ResponseEntity.ok(saved);
    }
}
