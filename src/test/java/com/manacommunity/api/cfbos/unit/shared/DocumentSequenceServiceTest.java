package com.manacommunity.api.cfbos.unit.shared;

import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.sequence.entity.DocumentSequence;
import com.manacommunity.api.cfbos.shared.sequence.repository.DocumentSequenceRepository;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSequenceService")
class DocumentSequenceServiceTest {

    @Mock
    private DocumentSequenceRepository documentSequenceRepository;

    @InjectMocks
    private DocumentSequenceService documentSequenceService;

    @Test
    @DisplayName("should generate first sequence number for new document type")
    void shouldGenerateFirstSequenceNumber() {
        when(documentSequenceRepository.findByDocumentTypeAndFiscalYear(DocumentType.INVOICE, "2026-27"))
                .thenReturn(Optional.empty());
        when(documentSequenceRepository.save(any(DocumentSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String number = documentSequenceService.nextNumber(DocumentType.INVOICE, "2026-27");

        assertThat(number).isEqualTo("INV-2627-000001");
    }

    @Test
    @DisplayName("should increment existing sequence number")
    void shouldIncrementExistingSequence() {
        DocumentSequence existing = DocumentSequence.builder()
                .documentType(DocumentType.INVOICE)
                .prefix("INV")
                .fiscalYear("2026-27")
                .currentValue(41L)
                .paddingLength(6)
                .build();

        when(documentSequenceRepository.findByDocumentTypeAndFiscalYear(DocumentType.INVOICE, "2026-27"))
                .thenReturn(Optional.of(existing));
        when(documentSequenceRepository.save(any(DocumentSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String number = documentSequenceService.nextNumber(DocumentType.INVOICE, "2026-27");

        assertThat(number).isEqualTo("INV-2627-000042");
    }

    @Test
    @DisplayName("should generate credit note number with CN prefix")
    void shouldGenerateCreditNoteNumber() {
        when(documentSequenceRepository.findByDocumentTypeAndFiscalYear(DocumentType.CREDIT_NOTE, "2026-27"))
                .thenReturn(Optional.empty());
        when(documentSequenceRepository.save(any(DocumentSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String number = documentSequenceService.nextNumber(DocumentType.CREDIT_NOTE, "2026-27");

        assertThat(number).isEqualTo("CN-2627-000001");
    }
}
