package com.manacommunity.api.controller;

import com.manacommunity.api.dto.EventDepartmentDto;
import com.manacommunity.api.service.EventDepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/departments")
@RequiredArgsConstructor
public class EventDepartmentController {

    private final EventDepartmentService service;

    @GetMapping
    public ResponseEntity<List<EventDepartmentDto>> list(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long eventId) {
        return ResponseEntity.ok(service.list(communityId, eventId));
    }

    @PostMapping
    public ResponseEntity<EventDepartmentDto> create(@RequestBody EventDepartmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDepartmentDto> update(
            @PathVariable Long id,
            @RequestBody EventDepartmentDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
