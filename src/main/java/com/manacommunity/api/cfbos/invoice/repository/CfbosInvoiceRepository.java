package com.manacommunity.api.cfbos.invoice.repository;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CfbosInvoiceRepository extends JpaRepository<CfbosInvoice, Long> { Optional<CfbosInvoice> findByInvoiceNumber(String invoiceNumber); List<CfbosInvoice> findByResidentId(Long residentId); List<CfbosInvoice> findByPropertyId(Long propertyId); List<CfbosInvoice> findByStatus(InvoiceStatus status); }
