package com.manacommunity.api.cfbos.unit.tax;

import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.dto.TdsCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import com.manacommunity.api.cfbos.tax.entity.TdsSection;
import com.manacommunity.api.cfbos.tax.repository.TdsSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxEngine")
class TaxEngineTest {

    @Mock
    private TdsSectionRepository tdsSectionRepository;

    @InjectMocks
    private TaxEngine taxEngine;

    @Nested
    @DisplayName("GST Calculation")
    class GstCalculation {

        @Test
        @DisplayName("should calculate intra-state GST at 9% CGST + 9% SGST")
        void shouldCalculateIntraStateGst() {
            GstCalculationResult result = taxEngine.calculateGst(
                    new BigDecimal("5000.00"),
                    new BigDecimal("9.00"),
                    new BigDecimal("9.00")
            );

            assertThat(result.getTaxableAmount()).isEqualByComparingTo("5000.00");
            assertThat(result.getCgstRate()).isEqualByComparingTo("9.00");
            assertThat(result.getCgstAmount()).isEqualByComparingTo("450.00");
            assertThat(result.getSgstRate()).isEqualByComparingTo("9.00");
            assertThat(result.getSgstAmount()).isEqualByComparingTo("450.00");
            assertThat(result.getTotalTax()).isEqualByComparingTo("900.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("5900.00");
        }

        @Test
        @DisplayName("should calculate GST with zero tax rate")
        void shouldCalculateZeroGst() {
            GstCalculationResult result = taxEngine.calculateGst(
                    new BigDecimal("1000.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            assertThat(result.getTotalTax()).isEqualByComparingTo("0.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("should round GST amounts to 2 decimal places")
        void shouldRoundGstAmounts() {
            GstCalculationResult result = taxEngine.calculateGst(
                    new BigDecimal("3333.33"),
                    new BigDecimal("9.00"),
                    new BigDecimal("9.00")
            );

            assertThat(result.getCgstAmount()).isEqualByComparingTo("300.00");
            assertThat(result.getSgstAmount()).isEqualByComparingTo("300.00");
        }
    }

    @Nested
    @DisplayName("TDS Calculation")
    class TdsCalculation {

        @Test
        @DisplayName("should calculate TDS for contractor payment (194C individual at 1%)")
        void shouldCalculateTdsForContractor() {
            TdsSection section = TdsSection.builder()
                    .sectionCode("194C")
                    .individualRate(new BigDecimal("1.00"))
                    .companyRate(new BigDecimal("2.00"))
                    .thresholdAmount(new BigDecimal("30000.00"))
                    .build();

            when(tdsSectionRepository.findBySectionCode("194C"))
                    .thenReturn(Optional.of(section));

            TdsCalculationResult result = taxEngine.calculateTds(
                    new BigDecimal("50000.00"), "194C", "INDIVIDUAL"
            );

            assertThat(result.getGrossAmount()).isEqualByComparingTo("50000.00");
            assertThat(result.getTdsRate()).isEqualByComparingTo("1.00");
            assertThat(result.getTdsAmount()).isEqualByComparingTo("500.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("49500.00");
        }

        @Test
        @DisplayName("should use company rate for non-individual payee")
        void shouldUseCompanyRate() {
            TdsSection section = TdsSection.builder()
                    .sectionCode("194C")
                    .individualRate(new BigDecimal("1.00"))
                    .companyRate(new BigDecimal("2.00"))
                    .thresholdAmount(new BigDecimal("30000.00"))
                    .build();

            when(tdsSectionRepository.findBySectionCode("194C"))
                    .thenReturn(Optional.of(section));

            TdsCalculationResult result = taxEngine.calculateTds(
                    new BigDecimal("100000.00"), "194C", "COMPANY"
            );

            assertThat(result.getTdsRate()).isEqualByComparingTo("2.00");
            assertThat(result.getTdsAmount()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("should return zero TDS when amount is below threshold")
        void shouldReturnZeroTdsBelowThreshold() {
            TdsSection section = TdsSection.builder()
                    .sectionCode("194C")
                    .individualRate(new BigDecimal("1.00"))
                    .companyRate(new BigDecimal("2.00"))
                    .thresholdAmount(new BigDecimal("30000.00"))
                    .build();

            when(tdsSectionRepository.findBySectionCode("194C"))
                    .thenReturn(Optional.of(section));

            TdsCalculationResult result = taxEngine.calculateTds(
                    new BigDecimal("25000.00"), "194C", "INDIVIDUAL"
            );

            assertThat(result.getTdsAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("25000.00");
        }
    }
}
