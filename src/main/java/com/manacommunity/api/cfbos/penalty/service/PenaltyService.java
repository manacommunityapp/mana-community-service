package com.manacommunity.api.cfbos.penalty.service;

import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.penalty.dto.PenaltyDto;
import com.manacommunity.api.cfbos.penalty.engine.PenaltyEngine;
import com.manacommunity.api.cfbos.penalty.entity.Penalty;
import com.manacommunity.api.cfbos.penalty.entity.PenaltyConfig;
import com.manacommunity.api.cfbos.penalty.repository.PenaltyConfigRepository;
import com.manacommunity.api.cfbos.penalty.repository.PenaltyRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyEngine penaltyEngine;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyConfigRepository configRepository;
    private final CfbosInvoiceRepository invoiceRepository;

    @Transactional
    public Optional<PenaltyDto> assessPenalty(Long invoiceId) {
        CfbosInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Invoice", invoiceId));
        return penaltyEngine.calculatePenalty(invoice, LocalDate.now()).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PenaltyDto> getPenaltiesForResident(Long residentId) {
        return penaltyRepository.findByResidentId(residentId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PenaltyDto> getPenaltiesForInvoice(Long invoiceId) {
        return penaltyRepository.findByInvoiceId(invoiceId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PenaltyConfig saveConfig(PenaltyConfig config) {
        return configRepository.save(config);
    }

    @Transactional(readOnly = true)
    public Optional<PenaltyConfig> getActiveConfig() {
        return configRepository.findByIsActiveTrue();
    }

    private PenaltyDto toDto(Penalty p) {
        return PenaltyDto.builder()
                .id(p.getId())
                .invoiceId(p.getInvoiceId())
                .residentId(p.getResidentId())
                .penaltyConfigId(p.getPenaltyConfig() != null ? p.getPenaltyConfig().getId() : null)
                .penaltyConfigName(p.getPenaltyConfig() != null ? p.getPenaltyConfig().getName() : "")
                .penaltyType(p.getPenaltyType())
                .amount(p.getAmount())
                .calculatedDate(p.getCalculatedDate())
                .appliedToInvoiceId(p.getAppliedToInvoiceId())
                .status(p.getStatus())
                .build();
    }
}
