package com.manacommunity.api.controller;

import com.manacommunity.api.sports.dto.SportsEventVenueConfigDto;
import com.manacommunity.api.service.SportsEventVenueConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/events/venues/config")
@RequiredArgsConstructor
public class SportsEventVenueConfigController {

    private final SportsEventVenueConfigService venueConfigService;

    @GetMapping
    public ResponseEntity<SportsEventVenueConfigDto> getVenueConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long eventId
) {
        Long communityId = principal.getCommunityId();
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
