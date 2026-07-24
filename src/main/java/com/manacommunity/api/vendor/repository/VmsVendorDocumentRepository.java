package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VmsVendorDocumentRepository extends JpaRepository<VmsVendorDocument, Long> {
    List<VmsVendorDocument> findByVendorId(Long vendorId);
    List<VmsVendorDocument> findByVendorIdAndStatus(Long vendorId, String status);
    List<VmsVendorDocument> findByExpiryDateBeforeAndStatus(LocalDate date, String status);
}
