package com.manacommunity.api.cfbos.unit.penalty;

import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.penalty.engine.PenaltyEngine;
import com.manacommunity.api.cfbos.penalty.entity.Penalty;
import com.manacommunity.api.cfbos.penalty.entity.PenaltyConfig;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyType;
import com.manacommunity.api.cfbos.penalty.repository.PenaltyConfigRepository;
import com.manacommunity.api.cfbos.penalty.repository.PenaltyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PenaltyEngineTest {

    @Mock private PenaltyConfigRepository penaltyConfigRepository;
    @Mock private PenaltyRepository penaltyRepository;

    private PenaltyEngine penaltyEngine;

    @BeforeEach
    void setUp() {
        penaltyEngine = new PenaltyEngine(penaltyConfigRepository, penaltyRepository);
    }

    @Test
    @DisplayName("Overdue invoice past grace period incurs late fee")
    void calculatePenaltyPastGracePeriod() {
        PenaltyConfig config = PenaltyConfig.builder()
                .id(1L)
                .name("Standard Late Fee")
                .gracePeriodDays(5)
                .lateFeeType(PenaltyType.PERCENTAGE)
                .lateFeePercentage(new BigDecimal("5.00"))
                .isActive(true)
                .build();

        CfbosInvoice invoice = CfbosInvoice.builder()
                .id(100L)
                .residentId(50L)
                .dueDate(LocalDate.of(2026, 4, 15))
                .balanceDue(new BigDecimal("10000.00"))
                .build();

        when(penaltyConfigRepository.findByIsActiveTrue()).thenReturn(Optional.of(config));
        when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));

        // As of 2026-04-25 (10 days after due date, > 5 grace days)
        Optional<Penalty> penalty = penaltyEngine.calculatePenalty(invoice, LocalDate.of(2026, 4, 25));

        assertThat(penalty).isPresent();
        assertThat(penalty.get().getAmount()).isEqualByComparingTo(new BigDecimal("500.00")); // 5% of 10000
    }
}
