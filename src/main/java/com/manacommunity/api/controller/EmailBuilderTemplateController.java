package com.manacommunity.api.controller;

import com.manacommunity.api.dto.email.EmailBuilderTemplateDtos.AssetResponse;
import com.manacommunity.api.dto.email.EmailBuilderTemplateDtos.TemplateRequest;
import com.manacommunity.api.dto.email.EmailBuilderTemplateDtos.TemplateResponse;
import com.manacommunity.api.email.EmailBuilderTemplateService;
import com.manacommunity.api.exception.InvalidFileUploadException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Backend for the drag-and-drop email template builder (GrapesJS). Distinct
 * from the code-driven templates under /api/admin/email — these are freeform,
 * named templates a community admin composes visually.
 */
@RestController
@RequestMapping("/api/email/templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','SPORTS_ADMIN')")
public class EmailBuilderTemplateController {

    private static final long MAX_ASSET_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/svg+xml");

    private final EmailBuilderTemplateService service;

    @GetMapping
    public List<TemplateResponse> list(@RequestParam Long communityId) {
        return service.list(communityId);
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest req) {
        TemplateRequest withoutId = new TemplateRequest(null, req.communityId(), req.templateName(),
                req.subject(), req.html(), req.css(), req.jsonLayout(), req.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(withoutId));
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@PathVariable Long id, @Valid @RequestBody TemplateRequest req) {
        TemplateRequest withId = new TemplateRequest(id, req.communityId(), req.templateName(),
                req.subject(), req.html(), req.css(), req.jsonLayout(), req.status());
        return service.save(withId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * No object storage is wired up in this codebase (see ExpenseService's mocked
     * receipt URL) — images are inlined as base64 data URIs so the builder canvas
     * and rendered emails actually display them, rather than a link to nowhere.
     */
    @PostMapping(value = "/assets", consumes = "multipart/form-data")
    public AssetResponse uploadAsset(@RequestParam("file") MultipartFile file,
                                      @RequestParam("communityId") Long communityId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException("No file provided");
        }
        if (file.getSize() > MAX_ASSET_BYTES) {
            throw new InvalidFileUploadException("Image exceeds the 2MB size limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileUploadException("Unsupported image type: " + contentType);
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String url = "data:" + contentType + ";base64," + base64;
            return new AssetResponse(url, file.getOriginalFilename());
        } catch (Exception e) {
            throw new InvalidFileUploadException("Failed to read uploaded file");
        }
    }
}
