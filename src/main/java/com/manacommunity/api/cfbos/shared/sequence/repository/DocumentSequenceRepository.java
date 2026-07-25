package com.manacommunity.api.cfbos.shared.sequence.repository;

import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.sequence.entity.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {
    Optional<DocumentSequence> findByDocumentTypeAndFiscalYear(DocumentType documentType, String fiscalYear);
}
