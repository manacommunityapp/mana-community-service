package com.manacommunity.api.email.builder.controller;

import com.manacommunity.api.email.builder.dto.AssetUploadResponse;
import com.manacommunity.api.email.builder.dto.EmailTemplateRequest;
import com.manacommunity.api.email.builder.dto.EmailTemplateResponse;
import com.manacommunity.api.email.builder.service.EmailTemplateBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/email/templates")
@RequiredArgsConstructor
public class EmailTemplateBuilderController {

    private final EmailTemplateBuilderService service;

    @GetMapping
    public ResponseEntity<List<EmailTemplateResponse>> list(@RequestParam Long communityId) {
        List<EmailTemplateResponse> templates = service.listByCommunity(communityId)
                .stream()
                .map(EmailTemplateResponse::from)
                .toList();
        return ResponseEntity.ok(templates);
    }

    @PostMapping
    public ResponseEntity<EmailTemplateResponse> create(@Valid @RequestBody EmailTemplateRequest request) {
        var saved = service.save(request);
        return ResponseEntity.ok(EmailTemplateResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody EmailTemplateRequest request) {
        var withId = new EmailTemplateRequest(
                id,
                request.communityId(),
                request.templateName(),
                request.subject(),
                request.html(),
                request.css(),
                request.layoutJson(),
                request.jsonLayout(),
                request.status(),
                request.category(),
                request.tags(),
                request.moduleKey(),
                request.menuKey(),
                request.menuLabel(),
                request.subMenuKey(),
                request.subMenuLabel(),
                request.useCase(),
                request.triggerKey(),
                request.templateKey(),
                request.themeName(),
                request.themeJson(),
                request.generatedCss()
        );
        var saved = service.save(withId);
        return ResponseEntity.ok(EmailTemplateResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetUploadResponse> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("communityId") Long communityId) {
        AssetUploadResponse response = service.uploadAsset(file, communityId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assets/{communityId}/{filename:.+}")
    public ResponseEntity<Resource> serveAsset(@PathVariable Long communityId,
                                               @PathVariable String filename) {
        String key = communityId + "/" + filename;
        Path path = service.resolveAssetPath(key);
        Resource resource = new FileSystemResource(path);

        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (Exception e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(resource);
    }
}
