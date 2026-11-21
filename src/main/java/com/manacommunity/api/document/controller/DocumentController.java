package com.manacommunity.api.document.controller;

import com.manacommunity.api.document.dto.*;
import com.manacommunity.api.document.entity.*;
import com.manacommunity.api.document.repository.DocumentAccessGrantRepository;
import com.manacommunity.api.document.service.DocumentService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentAccessGrantRepository grantRepo;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> upload(
            @RequestPart("metadata") @Valid DocumentUploadRequest request,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(request, file, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDocument(id, principal));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<DocumentSignedUrlResponse> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return ResponseEntity.ok(documentService.generateSignedUrl(id, principal, request));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionDto>> versions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getVersions(id, principal));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentVersionDto> addVersion(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.addVersion(id, file, principal));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<DocumentDto>> byEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.listByEntity(entityType, entityId, principal));
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> list(
            @RequestParam(required = false) DocumentCategory category,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.listByCommunity(principal.getCommunityId(), category, principal));
    }

    @PostMapping("/{id}/access-grants")
    public ResponseEntity<Void> grantAccess(
            @PathVariable Long id,
            @RequestBody @Valid DocumentAccessGrantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentAccessGrant grant = new DocumentAccessGrant();
        grant.setDocumentId(id);
        grant.setGranteeType(request.granteeType());
        grant.setGranteeId(request.granteeId());
        grant.setPermission(request.permission());
        grant.setGrantedBy(principal.getUserId());
        grant.setGrantedAt(Instant.now());
        if (request.expiresAt() != null)
            grant.setExpiresAt(request.expiresAt().atStartOfDay(ZoneOffset.UTC).toInstant());
        grantRepo.save(grant);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/access-grants/{grantId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long id,
            @PathVariable Long grantId) {
        grantRepo.deleteById(grantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit-log")
    public ResponseEntity<List<DocumentDownloadLog>> auditLog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getAuditLog(id, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.softDelete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/versions/{versionId}/scan-result")
    public ResponseEntity<Void> scanResult(
            @PathVariable Long versionId,
            @RequestBody ScanResultRequest request) {
        documentService.recordScanResult(versionId, request.result());
        return ResponseEntity.ok().build();
    }
}
