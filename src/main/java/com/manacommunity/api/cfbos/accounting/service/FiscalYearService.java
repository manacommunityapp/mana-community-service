package com.manacommunity.api.cfbos.accounting.service;

import com.manacommunity.api.cfbos.accounting.dto.FiscalYearDto;
import com.manacommunity.api.cfbos.accounting.entity.AccountingPeriod;
import com.manacommunity.api.cfbos.accounting.entity.FiscalYear;
import com.manacommunity.api.cfbos.accounting.repository.AccountingPeriodRepository;
import com.manacommunity.api.cfbos.accounting.repository.FiscalYearRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;

    @Transactional(readOnly = true)
    public List<FiscalYearDto> getAll() {
        return fiscalYearRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public FiscalYearDto create(FiscalYearDto dto) {
        FiscalYear fy = FiscalYear.builder()
                .name(dto.getName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .build();
        fy = fiscalYearRepository.save(fy);

        LocalDate periodStart = fy.getStartDate();
        for (int i = 1; i <= 12; i++) {
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            if (periodEnd.isAfter(fy.getEndDate())) {
                periodEnd = fy.getEndDate();
            }
            AccountingPeriod period = AccountingPeriod.builder()
                    .fiscalYear(fy)
                    .name(periodStart.getMonth().name() + " " + periodStart.getYear())
                    .startDate(periodStart)
                    .endDate(periodEnd)
                    .periodNumber(i)
                    .build();
            accountingPeriodRepository.save(period);
            periodStart = periodEnd.plusDays(1);
            if (periodStart.isAfter(fy.getEndDate())) break;
        }

        return toDto(fy);
    }

    @Transactional(readOnly = true)
    public FiscalYear getCurrentFiscalYear() {
        return fiscalYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new CfbosResourceNotFoundException("Current FiscalYear", 0L));
    }

    private FiscalYearDto toDto(FiscalYear e) {
        return FiscalYearDto.builder()
                .id(e.getId()).name(e.getName())
                .startDate(e.getStartDate()).endDate(e.getEndDate())
                .status(e.getStatus()).isCurrent(e.getIsCurrent())
                .build();
    }
}
