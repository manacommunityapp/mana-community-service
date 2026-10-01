package com.manacommunity.api.cfbos.billing.engine;

import com.manacommunity.api.cfbos.billing.dto.BillingRunRequest;
import com.manacommunity.api.cfbos.billing.entity.*;
import com.manacommunity.api.cfbos.billing.enums.BillingRunStatus;
import com.manacommunity.api.cfbos.billing.repository.*;
import com.manacommunity.api.cfbos.charge.dto.ChargeCalculationResult;
import com.manacommunity.api.cfbos.charge.dto.PropertyContext;
import com.manacommunity.api.cfbos.charge.engine.ChargeCalculationEngine;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingEngine {

    private final BillingRuleRepository billingRuleRepository;
    private final BillingScheduleRepository billingScheduleRepository;
    private final BillingRunRepository billingRunRepository;
    private final BillingRunLineRepository billingRunLineRepository;
    private final ChargeCalculationEngine chargeCalculationEngine;
    private final TaxEngine taxEngine;
    private final DocumentSequenceService documentSequenceService;

    private static final BigDecimal DEFAULT_CGST_RATE = new BigDecimal("9.00");
    private static final BigDecimal DEFAULT_SGST_RATE = new BigDecimal("9.00");

    @Transactional
    public BillingRun executeBillingRun(BillingRunRequest request, List<PropertyContext> properties) {
        BillingSchedule schedule = null;
        if (request.getBillingScheduleId() != null) {
            schedule = billingScheduleRepository.findById(request.getBillingScheduleId())
                    .orElseThrow(() -> new CfbosException("Billing schedule not found: " + request.getBillingScheduleId()));
        }

        List<BillingRule> activeRules = schedule != null ?
                billingRuleRepository.findByBillingScheduleIdAndIsActiveTrue(schedule.getId()) :
                billingRuleRepository.findActiveRulesForDate(request.getBillingPeriodStart());

        String fiscalYear = String.valueOf(request.getBillingPeriodStart().getYear());
        String runNumber = documentSequenceService.nextNumber(DocumentType.BILLING_RUN, fiscalYear);

        BillingRun run = BillingRun.builder()
                .runNumber(runNumber)
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .billingSchedule(schedule)
                .runType(request.getRunType() != null ? request.getRunType() : "REGULAR")
                .status(BillingRunStatus.CALCULATING)
                .autoSend(Boolean.TRUE.equals(request.getAutoSend()))
                .executedBy(request.getExecutedBy())
                .executedAt(LocalDateTime.now())
                .lines(new ArrayList<>())
                .build();

        BigDecimal runTotalAmount = BigDecimal.ZERO;
        BigDecimal runTotalTax = BigDecimal.ZERO;
        Set<Long> processedProperties = new HashSet<>();

        for (PropertyContext prop : properties) {
            if (request.getPropertyIds() != null && !request.getPropertyIds().isEmpty() &&
                    !request.getPropertyIds().contains(prop.getPropertyId())) {
                continue;
            }

            for (BillingRule rule : activeRules) {
                if (!matchesConditions(rule, prop)) continue;

                ChargeCalculationResult chargeResult = chargeCalculationEngine.calculate(
                        rule.getCalculationMethod(),
                        rule.getFixedAmount(),
                        rule.getRatePerUnit(),
                        prop
                );

                BigDecimal lineAmount = chargeResult.getAmount();
                BigDecimal lineTax = BigDecimal.ZERO;

                if (Boolean.TRUE.equals(rule.getIsTaxable())) {
                    GstCalculationResult gst = taxEngine.calculateGst(lineAmount, DEFAULT_CGST_RATE, DEFAULT_SGST_RATE);
                    lineTax = gst.getTotalTax();
                }

                BigDecimal lineTotal = lineAmount.add(lineTax);

                BillingRunLine line = BillingRunLine.builder()
                        .billingRun(run)
                        .propertyId(prop.getPropertyId())
                        .residentId(prop.getResidentId() != null ? prop.getResidentId() : 0L)
                        .billingRule(rule)
                        .chargeType(rule.getChargeType())
                        .description(rule.getName() + " - " + prop.getPropertyNumber())
                        .quantity(prop.getArea() != null ? prop.getArea() : BigDecimal.ONE)
                        .rate(rule.getRatePerUnit() != null ? rule.getRatePerUnit() : rule.getFixedAmount())
                        .amount(lineAmount)
                        .taxAmount(lineTax)
                        .totalAmount(lineTotal)
                        .calculationDetails(chargeResult.getBreakdown())
                        .build();

                run.getLines().add(line);
                runTotalAmount = runTotalAmount.add(lineAmount);
                runTotalTax = runTotalTax.add(lineTax);
                processedProperties.add(prop.getPropertyId());
            }
        }

        run.setTotalProperties(processedProperties.size());
        run.setTotalAmount(runTotalAmount.add(runTotalTax));
        run.setTotalTax(runTotalTax);
        run.setStatus(BillingRunStatus.COMPLETED);

        return billingRunRepository.save(run);
    }

    private boolean matchesConditions(BillingRule rule, PropertyContext prop) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) return true;
        for (BillingRuleCondition cond : rule.getConditions()) {
            if ("propertyType".equalsIgnoreCase(cond.getFieldName())) {
                if (!cond.getFieldValue().equalsIgnoreCase(prop.getPropertyType())) return false;
            } else if ("occupancyStatus".equalsIgnoreCase(cond.getFieldName())) {
                if (!cond.getFieldValue().equalsIgnoreCase(prop.getOccupancyStatus())) return false;
            }
        }
        return true;
    }
}
