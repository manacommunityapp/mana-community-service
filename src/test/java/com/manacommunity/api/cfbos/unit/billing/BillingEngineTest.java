package com.manacommunity.api.cfbos.unit.billing;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.cfbos.billing.dto.BillingRunRequest;
import com.manacommunity.api.cfbos.billing.engine.BillingEngine;
import com.manacommunity.api.cfbos.billing.entity.*;
import com.manacommunity.api.cfbos.billing.enums.BillingRunStatus;
import com.manacommunity.api.cfbos.billing.enums.BillingScheduleFrequency;
import com.manacommunity.api.cfbos.billing.repository.*;
import com.manacommunity.api.cfbos.charge.dto.ChargeCalculationResult;
import com.manacommunity.api.cfbos.charge.dto.PropertyContext;
import com.manacommunity.api.cfbos.charge.engine.ChargeCalculationEngine;
import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingEngineTest {

    @Mock private BillingRuleRepository billingRuleRepository;
    @Mock private BillingScheduleRepository billingScheduleRepository;
    @Mock private BillingRunRepository billingRunRepository;
    @Mock private BillingRunLineRepository billingRunLineRepository;
    @Mock private ChargeCalculationEngine chargeCalculationEngine;
    @Mock private TaxEngine taxEngine;
    @Mock private DocumentSequenceService documentSequenceService;

    private BillingEngine billingEngine;

    @BeforeEach
    void setUp() {
        billingEngine = new BillingEngine(
                billingRuleRepository, billingScheduleRepository, billingRunRepository,
                billingRunLineRepository, chargeCalculationEngine, taxEngine, documentSequenceService
        );
    }

    @Test
    @DisplayName("Execute billing run generates lines and calculates total with GST")
    void executeBillingRunCalculatesTotals() {
        BillingSchedule schedule = BillingSchedule.builder()
                .id(1L).name("Monthly Maintenance").frequency(BillingScheduleFrequency.MONTHLY).build();

        ChargeHead head = ChargeHead.builder().id(1L).code("MAINT").name("Maintenance").build();
        ChargeType type = ChargeType.builder().id(1L).chargeHead(head).code("MAINT_FLAT").name("Flat Rate").defaultHsnSacCode("9995").isTaxable(true).build();

        BillingRule rule = BillingRule.builder()
                .id(1L)
                .name("Standard Maintenance")
                .chargeType(type)
                .calculationMethod(CalculationMethod.FIXED)
                .fixedAmount(new BigDecimal("3000.00"))
                .isTaxable(true)
                .build();

        PropertyContext prop = PropertyContext.builder()
                .propertyId(10L)
                .residentId(100L)
                .propertyNumber("A-101")
                .propertyType("APARTMENT")
                .occupancyStatus("OCCUPIED")
                .area(new BigDecimal("1200"))
                .build();

        when(billingScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(billingRuleRepository.findByBillingScheduleIdAndIsActiveTrue(1L)).thenReturn(List.of(rule));
        when(documentSequenceService.nextNumber(any(), anyString())).thenReturn("BR-2026-0001");
        when(chargeCalculationEngine.calculate(any(), any(), any(), any()))
                .thenReturn(ChargeCalculationResult.builder().amount(new BigDecimal("3000.00")).breakdown("Fixed 3000").build());

        when(taxEngine.calculateGst(any(), any(), any()))
                .thenReturn(GstCalculationResult.builder()
                        .taxableAmount(new BigDecimal("3000.00"))
                        .cgstRate(new BigDecimal("9.00"))
                        .cgstAmount(new BigDecimal("270.00"))
                        .sgstRate(new BigDecimal("9.00"))
                        .sgstAmount(new BigDecimal("270.00"))
                        .totalTax(new BigDecimal("540.00"))
                        .build());

        when(billingRunRepository.save(any(BillingRun.class))).thenAnswer(i -> i.getArgument(0));

        BillingRunRequest req = BillingRunRequest.builder()
                .billingScheduleId(1L)
                .billingPeriodStart(LocalDate.of(2026, 4, 1))
                .billingPeriodEnd(LocalDate.of(2026, 4, 30))
                .build();

        BillingRun run = billingEngine.executeBillingRun(req, List.of(prop));

        assertThat(run.getStatus()).isEqualTo(BillingRunStatus.COMPLETED);
        assertThat(run.getTotalProperties()).isEqualTo(1);
        assertThat(run.getTotalTax()).isEqualByComparingTo(new BigDecimal("540.00"));
        assertThat(run.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3540.00"));
        assertThat(run.getLines()).hasSize(1);
    }
}
