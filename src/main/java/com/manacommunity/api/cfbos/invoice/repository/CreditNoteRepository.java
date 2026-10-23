package com.manacommunity.api.cfbos.invoice.repository;
import com.manacommunity.api.cfbos.invoice.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> { Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber); List<CreditNote> findByInvoiceId(Long invoiceId); }
