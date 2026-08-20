package com.manacommunity.api.repository;

import com.manacommunity.api.model.VendorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, Long> {
    java.util.List<VendorInvoice> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    java.util.Optional<VendorInvoice> findByIdAndCommunityId(Long id, Long communityId);
    long countByStatus(com.manacommunity.api.model.InvoiceStatus status);

    long countByPaymentStatus(com.manacommunity.api.model.PaymentStatus paymentStatus);

    @Query("select coalesce(sum(i.totalAmount), 0) from VendorInvoice i where i.status = com.manacommunity.api.model.InvoiceStatus.APPROVED")
    BigDecimal sumApprovedTotalAmount();

    @Query("select coalesce(sum(i.totalAmount), 0) from VendorInvoice i where i.paymentStatus = com.manacommunity.api.model.PaymentStatus.PENDING")
    BigDecimal sumPendingPaymentAmount();
}
