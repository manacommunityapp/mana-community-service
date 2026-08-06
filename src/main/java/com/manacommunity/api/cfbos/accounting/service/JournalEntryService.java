package com.manacommunity.api.cfbos.accounting.service;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryDto;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.accounting.repository.JournalEntryRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;

    @Transactional(readOnly = true)
    public Page<JournalEntryDto> list(LocalDate from, LocalDate to, Pageable pageable) {
        return journalEntryRepository.findByEntryDateBetween(from, to, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public JournalEntryDto getById(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("JournalEntry", id));
        return toDto(entry);
    }

    private JournalEntryDto toDto(JournalEntry e) {
        return JournalEntryDto.builder()
                .id(e.getId()).entryNumber(e.getEntryNumber())
                .entryDate(e.getEntryDate()).entryType(e.getEntryType())
                .sourceModule(e.getSourceModule())
                .sourceDocumentType(e.getSourceDocumentType())
                .sourceDocumentId(e.getSourceDocumentId())
                .narration(e.getNarration())
                .totalDebit(e.getTotalDebit()).totalCredit(e.getTotalCredit())
                .status(e.getStatus()).postedAt(e.getPostedAt())
                .lines(e.getLines().stream().map(l -> JournalEntryDto.LineDto.builder()
                        .id(l.getId())
                        .accountCode(l.getAccount().getCode())
                        .accountName(l.getAccount().getName())
                        .debitAmount(l.getDebitAmount())
                        .creditAmount(l.getCreditAmount())
                        .narration(l.getNarration())
                        .build()).toList())
                .build();
    }
}
