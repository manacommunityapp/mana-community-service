package com.manacommunity.api.procurement.repository;

import com.manacommunity.api.procurement.entity.ProcVendorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcVendorInvoiceRepository extends JpaRepository<ProcVendorInvoice, Long> {

    Optional<ProcVendorInvoice> findByInvoiceNumber(String invoiceNumber);

    List<ProcVendorInvoice> findByPurchaseOrderId(Long purchaseOrderId);

    List<ProcVendorInvoice> findByCommunityId(Long communityId);

    List<ProcVendorInvoice> findByCommunityIdAndStatus(Long communityId, ProcVendorInvoice.InvoiceStatus status);
}
