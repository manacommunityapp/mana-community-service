package com.manacommunity.api.cfbos.shared.sequence.service;

import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.sequence.entity.DocumentSequence;
import com.manacommunity.api.cfbos.shared.sequence.repository.DocumentSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentSequenceService {

    private final DocumentSequenceRepository documentSequenceRepository;

    private static final Map<DocumentType, String> PREFIX_MAP = Map.ofEntries(
            Map.entry(DocumentType.INVOICE, "INV"),
            Map.entry(DocumentType.CREDIT_NOTE, "CN"),
            Map.entry(DocumentType.DEBIT_NOTE, "DN"),
            Map.entry(DocumentType.RECEIPT, "RCT"),
            Map.entry(DocumentType.REFUND, "RFD"),
            Map.entry(DocumentType.JOURNAL_ENTRY, "JE"),
            Map.entry(DocumentType.PURCHASE_ORDER, "PO"),
            Map.entry(DocumentType.VENDOR_INVOICE, "VI"),
            Map.entry(DocumentType.VENDOR_PAYMENT, "VP"),
            Map.entry(DocumentType.EXPENSE, "EXP"),
            Map.entry(DocumentType.BILLING_RUN, "BR"),
            Map.entry(DocumentType.DEMAND_NOTE, "DM"),
            Map.entry(DocumentType.PAYMENT_ADVICE, "PA"),
            Map.entry(DocumentType.GRN, "GRN")
    );

    @Transactional
    public String nextNumber(DocumentType type, String fiscalYear) {
        String prefix = PREFIX_MAP.getOrDefault(type, type.name().substring(0, 3));
        String fyShort = fiscalYear.replace("-", "").substring(2);

        DocumentSequence seq = documentSequenceRepository
                .findByDocumentTypeAndFiscalYear(type, fiscalYear)
                .orElse(DocumentSequence.builder()
                        .documentType(type)
                        .prefix(prefix)
                        .fiscalYear(fiscalYear)
                        .currentValue(0L)
                        .paddingLength(6)
                        .build());

        seq.setCurrentValue(seq.getCurrentValue() + 1);
        documentSequenceRepository.save(seq);

        String paddedNumber = String.format("%0" + seq.getPaddingLength() + "d", seq.getCurrentValue());
        return prefix + "-" + fyShort + "-" + paddedNumber;
    }
}
