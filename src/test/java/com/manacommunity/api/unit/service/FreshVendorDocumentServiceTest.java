package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.vendor.dto.UploadVendorDocumentRequest;
import com.manacommunity.api.fresh.vendor.dto.VendorDocumentDto;
import com.manacommunity.api.fresh.vendor.model.DocumentVerificationStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorDocument;
import com.manacommunity.api.fresh.vendor.model.VendorDocumentType;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorDocumentRepository;
import com.manacommunity.api.fresh.vendor.service.FreshVendorDocumentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshVendorDocumentServiceTest {

    @Mock private FreshVendorDocumentRepository documentRepository;
    @Mock private VmsVendorRepository vendorRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks
    private FreshVendorDocumentService documentService;

    private VmsVendor testVendor;
    private AppUser adminUser;

    @BeforeEach
    void setUp() {
        testVendor = VmsVendor.builder()
                .id(101L)
                .businessName("Annapoorna Batters")
                .build();

        adminUser = AppUser.builder()
                .id(1L)
                .fullName("Admin Quality Lead")
                .build();
    }

    @Test
    void testUploadDocument_SavesSuccessfully() {
        UploadVendorDocumentRequest req = UploadVendorDocumentRequest.builder()
                .vendorId(101L)
                .documentType(VendorDocumentType.FSSAI_LICENSE)
                .documentNumber("FSSAI-1122334455")
                .validFrom(LocalDate.of(2026, 1, 1))
                .validTo(LocalDate.of(2027, 1, 1))
                .fileKey("docs/fssai.pdf")
                .build();

        when(vendorRepository.findById(101L)).thenReturn(Optional.of(testVendor));
        when(documentRepository.findByVendorIdAndDocumentType(101L, VendorDocumentType.FSSAI_LICENSE))
                .thenReturn(Optional.empty());
        when(documentRepository.save(any(FreshVendorDocument.class))).thenAnswer(i -> {
            FreshVendorDocument d = i.getArgument(0);
            d.setId(50L);
            return d;
        });

        VendorDocumentDto result = documentService.uploadDocument(req);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals("Annapoorna Batters", result.getVendorBusinessName());
        assertEquals(VendorDocumentType.FSSAI_LICENSE, result.getDocumentType());
        assertEquals(DocumentVerificationStatus.PENDING_VERIFICATION, result.getStatus());
    }

    @Test
    void testVerifyDocument_Approve_UpdatesStatus() {
        FreshVendorDocument doc = FreshVendorDocument.builder()
                .id(50L)
                .vendor(testVendor)
                .documentType(VendorDocumentType.GST_CERTIFICATE)
                .status(DocumentVerificationStatus.PENDING_VERIFICATION)
                .build();

        when(documentRepository.findById(50L)).thenReturn(Optional.of(doc));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VendorDocumentDto result = documentService.verifyDocument(50L, true, null, 1L);

        assertNotNull(result);
        assertEquals(DocumentVerificationStatus.VERIFIED, result.getStatus());
        assertEquals("Admin Quality Lead", result.getVerifiedByUserName());
    }

    @Test
    void testVerifyDocument_Reject_SetsReason() {
        FreshVendorDocument doc = FreshVendorDocument.builder()
                .id(50L)
                .vendor(testVendor)
                .documentType(VendorDocumentType.FSSAI_LICENSE)
                .status(DocumentVerificationStatus.PENDING_VERIFICATION)
                .build();

        when(documentRepository.findById(50L)).thenReturn(Optional.of(doc));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VendorDocumentDto result = documentService.verifyDocument(50L, false, "Expired license certificate", 1L);

        assertNotNull(result);
        assertEquals(DocumentVerificationStatus.REJECTED, result.getStatus());
        assertEquals("Expired license certificate", result.getRejectionReason());
    }
}
