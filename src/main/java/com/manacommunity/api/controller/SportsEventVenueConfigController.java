package com.manacommunity.api.controller;

import com.manacommunity.api.sports.dto.SportsEventVenueConfigDto;
import com.manacommunity.api.service.SportsEventVenueConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/venues/config")
@RequiredArgsConstructor
public class SportsEventVenueConfigController {

    private final SportsEventVenueConfigService venueConfigService;

    @GetMapping
    public ResponseEntity<SportsEventVenueConfigDto> getVenueConfig(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long communityId) {
        SportsEventVenueConfigDto dto = venueConfigService.getVenueConfig(eventId, communityId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<SportsEventVenueConfigDto> saveVenueConfig(
            @RequestBody SportsEventVenueConfigDto dto) {
        SportsEventVenueConfigDto saved = venueConfigService.saveVenueConfig(dto);
        return ResponseEntity.ok(saved);
    }
}
