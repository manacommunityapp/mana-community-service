package com.manacommunity.api.cfbos.tax.engine;

import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.dto.TdsCalculationResult;
import com.manacommunity.api.cfbos.tax.entity.TdsSection;
import com.manacommunity.api.cfbos.tax.repository.TdsSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TaxEngine {

    private final TdsSectionRepository tdsSectionRepository;

    public GstCalculationResult calculateGst(BigDecimal taxableAmount,
                                              BigDecimal cgstRate,
                                              BigDecimal sgstRate) {
        BigDecimal cgstAmount = taxableAmount
                .multiply(cgstRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal sgstAmount = taxableAmount
                .multiply(sgstRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal totalTax = cgstAmount.add(sgstAmount);

        return GstCalculationResult.builder()
                .taxableAmount(taxableAmount)
                .cgstRate(cgstRate)
                .cgstAmount(cgstAmount)
                .sgstRate(sgstRate)
                .sgstAmount(sgstAmount)
                .totalTax(totalTax)
                .totalAmount(taxableAmount.add(totalTax))
                .build();
    }

    public TdsCalculationResult calculateTds(BigDecimal grossAmount,
                                              String tdsSectionCode,
                                              String payeeType) {
        TdsSection section = tdsSectionRepository.findBySectionCode(tdsSectionCode)
                .orElseThrow(() -> new CfbosResourceNotFoundException("TDS Section " + tdsSectionCode, 0L));

        if (section.getThresholdAmount() != null
                && grossAmount.compareTo(section.getThresholdAmount()) < 0) {
            return TdsCalculationResult.builder()
                    .grossAmount(grossAmount)
                    .tdsSection(tdsSectionCode)
                    .tdsRate(BigDecimal.ZERO)
                    .tdsAmount(BigDecimal.ZERO)
                    .netAmount(grossAmount)
                    .build();
        }

        BigDecimal rate = "INDIVIDUAL".equalsIgnoreCase(payeeType)
                ? section.getIndividualRate()
                : section.getCompanyRate();

        BigDecimal tdsAmount = grossAmount
                .multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return TdsCalculationResult.builder()
                .grossAmount(grossAmount)
                .tdsSection(tdsSectionCode)
                .tdsRate(rate)
                .tdsAmount(tdsAmount)
                .netAmount(grossAmount.subtract(tdsAmount))
                .build();
    }
}
