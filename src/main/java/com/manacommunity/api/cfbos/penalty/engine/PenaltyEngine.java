package com.manacommunity.api.cfbos.penalty.engine;

import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.penalty.entity.Penalty;
import com.manacommunity.api.cfbos.penalty.entity.PenaltyConfig;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyStatus;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyType;
import com.manacommunity.api.cfbos.penalty.repository.PenaltyConfigRepository;
import com.manacommunity.api.cfbos.penalty.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PenaltyEngine {

    private final PenaltyConfigRepository penaltyConfigRepository;
    private final PenaltyRepository penaltyRepository;

    @Transactional
    public Optional<Penalty> calculatePenalty(CfbosInvoice invoice, LocalDate asOfDate) {
        if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        PenaltyConfig config = penaltyConfigRepository.findByIsActiveTrue().orElse(null);
        if (config == null || !Boolean.TRUE.equals(config.getIsActive())) {
            return Optional.empty();
        }

        int graceDays = config.getGracePeriodDays() != null ? config.getGracePeriodDays() : 0;
        LocalDate graceCutoff = invoice.getDueDate().plusDays(graceDays);

        if (asOfDate.isBefore(graceCutoff) || asOfDate.isEqual(graceCutoff)) {
            return Optional.empty();
        }

        BigDecimal penaltyAmount = BigDecimal.ZERO;
        if (config.getLateFeeType() == PenaltyType.FIXED) {
            penaltyAmount = config.getLateFeeAmount() != null ? config.getLateFeeAmount() : BigDecimal.ZERO;
        } else if (config.getLateFeeType() == PenaltyType.PERCENTAGE) {
            BigDecimal pct = config.getLateFeePercentage() != null ? config.getLateFeePercentage() : BigDecimal.ZERO;
            penaltyAmount = invoice.getBalanceDue().multiply(pct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (config.getLateFeeType() == PenaltyType.DAILY_INTEREST) {
            long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate);
            BigDecimal annualRate = config.getInterestRateAnnual() != null ? config.getInterestRateAnnual() : new BigDecimal("18.00");
            BigDecimal dailyRate = annualRate.divide(new BigDecimal("36500"), 6, RoundingMode.HALF_UP);
            penaltyAmount = invoice.getBalanceDue().multiply(dailyRate).multiply(new BigDecimal(daysOverdue)).setScale(2, RoundingMode.HALF_UP);
        }

        if (config.getMaxPenaltyCap() != null && penaltyAmount.compareTo(config.getMaxPenaltyCap()) > 0) {
            penaltyAmount = config.getMaxPenaltyCap();
        }

        if (penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        Penalty penalty = Penalty.builder()
                .invoiceId(invoice.getId())
                .residentId(invoice.getResidentId())
                .penaltyConfig(config)
                .penaltyType(config.getLateFeeType())
                .amount(penaltyAmount)
                .calculatedDate(asOfDate)
                .status(PenaltyStatus.CALCULATED)
                .build();

        return Optional.of(penaltyRepository.save(penalty));
    }
}
