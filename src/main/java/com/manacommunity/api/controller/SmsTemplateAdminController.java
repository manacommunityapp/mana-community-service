package com.manacommunity.api.controller;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.notification.dto.SmsTemplateRequest;
import com.manacommunity.api.notification.entity.SmsTemplate;
import com.manacommunity.api.notification.enums.TemplateStatus;
import com.manacommunity.api.notification.repository.SmsTemplateRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sms/templates")
@PreAuthorize("hasAuthority('View Admin')")
@RequiredArgsConstructor
public class SmsTemplateAdminController {

    private final SmsTemplateRepository templateRepository;

    @GetMapping
    public ResponseEntity<List<SmsTemplate>> listAll() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmsTemplate> getById(@PathVariable Long id) {
        return ResponseEntity.ok(findOrThrow(id));
    }

    @PostMapping
    public ResponseEntity<SmsTemplate> create(@Valid @RequestBody SmsTemplateRequest request) {
        SmsTemplate template = SmsTemplate.builder()
                .templateCode(request.getTemplateCode())
                .name(request.getName())
                .body(request.getBody())
                .language(request.getLanguage())
                .messageType(request.getMessageType())
                .status(TemplateStatus.DRAFT)
                .dltTemplateId(request.getDltTemplateId())
                .unicode(request.isUnicode())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(templateRepository.save(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SmsTemplate> update(@PathVariable Long id,
                                               @Valid @RequestBody SmsTemplateRequest request) {
        SmsTemplate template = findOrThrow(id);
        template.setName(request.getName());
        template.setBody(request.getBody());
        template.setMessageType(request.getMessageType());
        template.setDltTemplateId(request.getDltTemplateId());
        template.setUnicode(request.isUnicode());
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SmsTemplate> approve(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        SmsTemplate template = findOrThrow(id);
        template.setStatus(TemplateStatus.APPROVED);
        template.setApprovedBy(principal.getUsername());
        template.setApprovedAt(LocalDateTime.now());
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<SmsTemplate> activate(@PathVariable Long id) {
        SmsTemplate template = findOrThrow(id);
        if (template.getStatus() != TemplateStatus.APPROVED) {
            throw new ManaCommunityException("Template must be APPROVED before activation",
                    HttpStatus.CONFLICT, "TEMPLATE_NOT_APPROVED");
        }
        template.setStatus(TemplateStatus.ACTIVE);
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<SmsTemplate> deactivate(@PathVariable Long id) {
        SmsTemplate template = findOrThrow(id);
        template.setStatus(TemplateStatus.INACTIVE);
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateRepository.delete(findOrThrow(id));
        return ResponseEntity.noContent().build();
    }

    private SmsTemplate findOrThrow(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ManaCommunityException("Template not found: " + id,
                        HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND"));
    }
}
