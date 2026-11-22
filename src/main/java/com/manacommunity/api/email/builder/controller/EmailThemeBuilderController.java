package com.manacommunity.api.email.builder.controller;

import com.manacommunity.api.email.builder.dto.EmailThemeRequest;
import com.manacommunity.api.email.builder.dto.EmailThemeResponse;
import com.manacommunity.api.email.builder.service.EmailTemplateBuilderService;
import com.manacommunity.api.email.builder.service.EmailThemeBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/email/themes")
@RequiredArgsConstructor
public class EmailThemeBuilderController {

    private final EmailThemeBuilderService themeService;
    private final EmailTemplateBuilderService templateService;

    @GetMapping
    public ResponseEntity<List<EmailThemeResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        List<EmailThemeResponse> themes = themeService.listByCommunity(communityId)
                .stream()
                .map(EmailThemeResponse::from)
                .toList();
        return ResponseEntity.ok(themes);
    }

    @PostMapping
    public ResponseEntity<EmailThemeResponse> create(@Valid @RequestBody EmailThemeRequest request) {
        var saved = themeService.save(request);
        return ResponseEntity.ok(EmailThemeResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailThemeResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody EmailThemeRequest request) {
        var withId = new EmailThemeRequest(id, request.communityId(), request.name(), request.themeJson(), request.isDefault());
        var saved = themeService.save(withId);
        return ResponseEntity.ok(EmailThemeResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        themeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{themeId}/apply")
    public ResponseEntity<Void> applyToTemplate(@PathVariable Long themeId,
                                                 @RequestParam Long templateId) {
        themeService.applyThemeToTemplate(themeId, templateId, templateService);
        return ResponseEntity.ok().build();
    }
}
