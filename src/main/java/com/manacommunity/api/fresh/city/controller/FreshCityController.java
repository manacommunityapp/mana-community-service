package com.manacommunity.api.fresh.city.controller;

import com.manacommunity.api.fresh.city.dto.CityDto;
import com.manacommunity.api.fresh.city.dto.ServiceZoneDto;
import com.manacommunity.api.fresh.city.dto.ServiceabilityResolutionDto;
import com.manacommunity.api.fresh.city.service.FreshCityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh")
@RequiredArgsConstructor
public class FreshCityController {

    private final FreshCityService cityService;

    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getActiveCities() {
        return ResponseEntity.ok(cityService.getAllActiveCities());
    }

    @GetMapping("/service-zones")
    public ResponseEntity<List<ServiceZoneDto>> getServiceZones(
            @RequestParam(required = false) Long cityId) {
        if (cityId != null) {
            return ResponseEntity.ok(cityService.getServiceZonesByCity(cityId));
        }
        return ResponseEntity.ok(cityService.getAllActiveCities().stream()
                .flatMap(c -> c.getServiceZones().stream())
                .toList());
    }

    @GetMapping("/serviceability")
    public ResponseEntity<ServiceabilityResolutionDto> resolveServiceability(
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(cityService.resolveServiceability(postalCode, communityId));
    }
}
