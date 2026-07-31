package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsInvoiceRepository extends JpaRepository<VmsInvoice, Long> {
    Page<VmsInvoice> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsInvoice> findByCommunityIdAndStatus(Long communityId, VmsInvoice.InvoiceStatus status, Pageable pageable);
    Page<VmsInvoice> findByVendorId(Long vendorId, Pageable pageable);
    Optional<VmsInvoice> findByIdAndCommunityId(Long id, Long communityId);
    Optional<VmsInvoice> findByInvoiceNumberAndCommunityId(String invoiceNumber, Long communityId);
}
