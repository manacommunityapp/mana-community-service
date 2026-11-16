package com.manacommunity.api.fresh.vendor.service;

import com.manacommunity.api.fresh.vendor.dto.UploadVendorDocumentRequest;
import com.manacommunity.api.fresh.vendor.dto.VendorDocumentDto;
import com.manacommunity.api.fresh.vendor.model.DocumentVerificationStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorDocument;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorDocumentRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreshVendorDocumentService {

    private final FreshVendorDocumentRepository documentRepository;
    private final VmsVendorRepository vendorRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public VendorDocumentDto uploadDocument(UploadVendorDocumentRequest request) {
        VmsVendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + request.getVendorId()));

        if (request.getFileUrl() != null && !request.getFileUrl().isBlank()) {
            validateDocumentUrl(request.getFileUrl());
        }

        FreshVendorDocument doc = documentRepository
                .findByVendorIdAndDocumentType(request.getVendorId(), request.getDocumentType())
                .orElseGet(() -> FreshVendorDocument.builder()
                        .vendor(vendor)
                        .documentType(request.getDocumentType())
                        .build());

        doc.setDocumentNumber(request.getDocumentNumber());
        doc.setValidFrom(request.getValidFrom());
        doc.setValidTo(request.getValidTo());
        doc.setFileKey(request.getFileKey());
        doc.setFileUrl(request.getFileUrl());
        doc.setStatus(DocumentVerificationStatus.PENDING_VERIFICATION);
        doc.setRejectionReason(null);

        FreshVendorDocument saved = documentRepository.save(doc);
        log.info("Uploaded compliance document {} for vendor {}", request.getDocumentType(), vendor.getBusinessName());
        return mapToDto(saved);
    }

    private void validateDocumentUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                    || host.startsWith("192.168.") || host.startsWith("10.") || "169.254.169.254".equals(host)) {
                throw new IllegalArgumentException("Invalid document URL origin: internal or loopback addresses disallowed.");
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid document URL format: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<VendorDocumentDto> getVendorDocuments(Long vendorId) {
        return documentRepository.findByVendorId(vendorId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public VendorDocumentDto verifyDocument(Long documentId, boolean approved, String rejectionReason, Long verifiedByUserId) {
        FreshVendorDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + documentId));

        AppUser reviewer = verifiedByUserId != null ? userRepository.findById(verifiedByUserId).orElse(null) : null;

        if (approved) {
            doc.setStatus(DocumentVerificationStatus.VERIFIED);
            doc.setRejectionReason(null);
        } else {
            doc.setStatus(DocumentVerificationStatus.REJECTED);
            doc.setRejectionReason(rejectionReason);
        }
        doc.setVerifiedByUser(reviewer);
        doc.setVerifiedAt(Instant.now());

        FreshVendorDocument saved = documentRepository.save(doc);
        if (auditService != null) {
            AuditAction action = approved ? AuditAction.FRESH_DOCUMENT_VERIFIED : AuditAction.FRESH_DOCUMENT_REJECTED;
            auditService.record(action, AuditModule.FRESH_COMMERCE, "FreshVendorDocument",
                    doc.getId().toString(), DocumentVerificationStatus.PENDING_VERIFICATION.name(), doc.getStatus().name());
        }

        log.info("Document {} for vendor {} verified with decision: {}", doc.getId(), doc.getVendor().getBusinessName(), doc.getStatus());
        return mapToDto(saved);
    }

    private VendorDocumentDto mapToDto(FreshVendorDocument doc) {
        return VendorDocumentDto.builder()
                .id(doc.getId())
                .vendorId(doc.getVendor().getId())
                .vendorBusinessName(doc.getVendor().getBusinessName())
                .documentType(doc.getDocumentType())
                .documentNumber(doc.getDocumentNumber())
                .validFrom(doc.getValidFrom())
                .validTo(doc.getValidTo())
                .fileKey(doc.getFileKey())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus())
                .verifiedByUserId(doc.getVerifiedByUser() != null ? doc.getVerifiedByUser().getId() : null)
                .verifiedByUserName(doc.getVerifiedByUser() != null ? doc.getVerifiedByUser().getFullName() : null)
                .verifiedAt(doc.getVerifiedAt())
                .rejectionReason(doc.getRejectionReason())
                .build();
    }
}
