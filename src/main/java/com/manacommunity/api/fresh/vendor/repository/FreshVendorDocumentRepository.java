package com.manacommunity.api.fresh.vendor.repository;

import com.manacommunity.api.fresh.vendor.model.DocumentVerificationStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorDocument;
import com.manacommunity.api.fresh.vendor.model.VendorDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshVendorDocumentRepository extends JpaRepository<FreshVendorDocument, Long> {
    List<FreshVendorDocument> findByVendorId(Long vendorId);
    Optional<FreshVendorDocument> findByVendorIdAndDocumentType(Long vendorId, VendorDocumentType documentType);
    List<FreshVendorDocument> findByStatus(DocumentVerificationStatus status);
}
