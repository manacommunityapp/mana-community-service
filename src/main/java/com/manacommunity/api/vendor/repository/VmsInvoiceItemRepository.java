package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VmsInvoiceItemRepository extends JpaRepository<VmsInvoiceItem, Long> {
    List<VmsInvoiceItem> findByInvoiceIdOrderBySortOrder(Long invoiceId);
}
