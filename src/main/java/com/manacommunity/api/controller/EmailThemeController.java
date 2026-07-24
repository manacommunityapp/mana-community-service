package com.manacommunity.api.controller;

import com.manacommunity.api.dto.email.EmailThemeDto;
import com.manacommunity.api.service.EmailThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email/themes")
@RequiredArgsConstructor
public class EmailThemeController {

    private final EmailThemeService themeService;

    @GetMapping
    public ResponseEntity<List<EmailThemeDto>> list(@RequestParam Long communityId) {
        return ResponseEntity.ok(themeService.listByCommunity(communityId));
    }

    @PostMapping
    public ResponseEntity<EmailThemeDto> create(@RequestBody EmailThemeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(themeService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailThemeDto> update(@PathVariable Long id, @RequestBody EmailThemeDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(themeService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        themeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> apply(
            @PathVariable Long id,
            @RequestParam Long templateId) {
        themeService.applyThemeToTemplate(id, templateId);
        return ResponseEntity.ok(Map.of("themeId", id, "templateId", templateId, "status", "applied"));
    }
}
