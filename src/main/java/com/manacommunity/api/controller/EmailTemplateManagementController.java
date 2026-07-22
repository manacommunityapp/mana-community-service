package com.manacommunity.api.controller;

import com.manacommunity.api.dto.email.EmailTemplateManagementDtos.*;
import com.manacommunity.api.email.EmailTemplateManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
public class EmailTemplateManagementController {

    private final EmailTemplateManagementService service;

    @GetMapping("/communities/{communityId}/branding")
    public BrandingResponse getBranding(@PathVariable Long communityId) {
        return service.getBranding(communityId);
    }

    @PutMapping("/communities/{communityId}/branding")
    public BrandingResponse saveBranding(
            @PathVariable Long communityId,
            @RequestBody BrandingRequest request
    ) {
        return service.saveBranding(communityId, request);
    }

    @GetMapping("/communities/{communityId}/headers")
    public List<HeaderFooterResponse> listHeaders(@PathVariable Long communityId) {
        return service.listHeaders(communityId);
    }

    @PostMapping("/communities/{communityId}/headers")
    public ResponseEntity<HeaderFooterResponse> createHeader(
            @PathVariable Long communityId,
            @Valid @RequestBody HeaderFooterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createHeader(communityId, request));
    }

    @PutMapping("/communities/{communityId}/headers/{headerId}")
    public HeaderFooterResponse updateHeader(
            @PathVariable Long communityId,
            @PathVariable Long headerId,
            @Valid @RequestBody HeaderFooterRequest request
    ) {
        return service.updateHeader(communityId, headerId, request);
    }

    @GetMapping("/communities/{communityId}/footers")
    public List<HeaderFooterResponse> listFooters(@PathVariable Long communityId) {
        return service.listFooters(communityId);
    }

    @PostMapping("/communities/{communityId}/footers")
    public ResponseEntity<HeaderFooterResponse> createFooter(
            @PathVariable Long communityId,
            @Valid @RequestBody HeaderFooterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFooter(communityId, request));
    }

    @PutMapping("/communities/{communityId}/footers/{footerId}")
    public HeaderFooterResponse updateFooter(
            @PathVariable Long communityId,
            @PathVariable Long footerId,
            @Valid @RequestBody HeaderFooterRequest request
    ) {
        return service.updateFooter(communityId, footerId, request);
    }

    @GetMapping("/communities/{communityId}/templates")
    public List<TemplateSummaryResponse> listTemplates(@PathVariable Long communityId) {
        return service.listTemplates(communityId);
    }

    @GetMapping("/communities/{communityId}/templates/{templateCode}")
    public TemplateDetailResponse getTemplate(
            @PathVariable Long communityId,
            @PathVariable String templateCode
    ) {
        return service.getTemplate(communityId, templateCode);
    }

    @PostMapping("/communities/{communityId}/templates")
    public ResponseEntity<TemplateDetailResponse> createTemplate(
            @PathVariable Long communityId,
            @Valid @RequestBody TemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTemplate(communityId, request));
    }

    @PutMapping("/communities/{communityId}/templates/{templateId}")
    public TemplateDetailResponse updateTemplate(
            @PathVariable Long communityId,
            @PathVariable Long templateId,
            @Valid @RequestBody TemplateRequest request
    ) {
        return service.updateTemplate(communityId, templateId, request);
    }

    @PostMapping("/communities/{communityId}/templates/{templateCode}/preview")
    public PreviewResponse previewActiveTemplate(
            @PathVariable Long communityId,
            @PathVariable String templateCode,
            @RequestBody(required = false) PreviewRequest request
    ) {
        Map<String, Object> data = request == null ? Map.of() : request.data();
        return service.previewActiveTemplate(communityId, templateCode, data);
    }

    @PostMapping("/communities/{communityId}/templates/by-id/{templateId}/preview")
    public PreviewResponse previewTemplate(
            @PathVariable Long communityId,
            @PathVariable Long templateId,
            @RequestBody(required = false) PreviewRequest request
    ) {
        Map<String, Object> data = request == null ? Map.of() : request.data();
        return service.previewTemplate(communityId, templateId, data);
    }

    @GetMapping("/dynamic-fields")
    public List<DynamicFieldResponse> listDynamicFields() {
        return service.listDynamicFields();
    }
}
