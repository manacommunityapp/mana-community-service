package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VmsPaymentRepository extends JpaRepository<VmsPayment, Long> {
    Page<VmsPayment> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsPayment> findByVendorId(Long vendorId, Pageable pageable);
    List<VmsPayment> findByInvoiceId(Long invoiceId);
    Optional<VmsPayment> findByIdAndCommunityId(Long id, Long communityId);
}
