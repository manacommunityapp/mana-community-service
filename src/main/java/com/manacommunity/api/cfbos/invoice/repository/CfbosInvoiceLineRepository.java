package com.manacommunity.api.cfbos.invoice.repository;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CfbosInvoiceLineRepository extends JpaRepository<CfbosInvoiceLine, Long> { List<CfbosInvoiceLine> findByInvoiceId(Long invoiceId); }
