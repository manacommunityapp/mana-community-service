package com.manacommunity.api.controller;

import com.manacommunity.api.dto.events.EventProspectusDto;
import com.manacommunity.api.service.EventProspectusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/prospectus")
@RequiredArgsConstructor
public class EventProspectusController {

    private final EventProspectusService prospectusService;

    @GetMapping
    public ResponseEntity<EventProspectusDto> getProspectus(@RequestParam(required = false) Long eventId) {
        return ResponseEntity.ok(prospectusService.getProspectus(eventId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventProspectusDto> getProspectusByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(prospectusService.getProspectus(eventId));
    }

    @PostMapping
    public ResponseEntity<EventProspectusDto> saveProspectus(@RequestBody EventProspectusDto dto) {
        return ResponseEntity.ok(prospectusService.saveProspectus(dto));
    }

    @PutMapping
    public ResponseEntity<EventProspectusDto> updateProspectus(@RequestBody EventProspectusDto dto) {
        return ResponseEntity.ok(prospectusService.saveProspectus(dto));
    }
}
