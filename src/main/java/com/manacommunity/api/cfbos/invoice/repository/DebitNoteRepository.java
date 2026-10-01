package com.manacommunity.api.cfbos.invoice.repository;
import com.manacommunity.api.cfbos.invoice.entity.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> { Optional<DebitNote> findByDebitNoteNumber(String debitNoteNumber); List<DebitNote> findByInvoiceId(Long invoiceId); }
