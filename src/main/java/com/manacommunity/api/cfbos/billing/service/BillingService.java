package com.manacommunity.api.cfbos.billing.service;
import com.manacommunity.api.cfbos.billing.dto.*;
import com.manacommunity.api.cfbos.billing.engine.BillingEngine;
import com.manacommunity.api.cfbos.billing.entity.*;
import com.manacommunity.api.cfbos.billing.repository.*;
import com.manacommunity.api.cfbos.charge.dto.PropertyContext;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service("cfbosBillingService")
@RequiredArgsConstructor
public class BillingService {
    private final BillingEngine billingEngine;
    private final BillingRunRepository billingRunRepository;
    private final BillingScheduleRepository billingScheduleRepository;
    private final ChargeHeadRepository chargeHeadRepository;
    private final ChargeTypeRepository chargeTypeRepository;

    @Transactional
    public BillingRunResponse runBilling(BillingRunRequest request, List<PropertyContext> properties) {
        BillingRun run = billingEngine.executeBillingRun(request, properties);
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public BillingRunResponse getBillingRunById(Long id) {
        BillingRun run = billingRunRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("BillingRun", id));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public List<BillingRunResponse> getAllBillingRuns() {
        return billingRunRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillingSchedule> getAllSchedules() { return billingScheduleRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<ChargeHead> getAllChargeHeads() { return chargeHeadRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<ChargeType> getAllChargeTypes() { return chargeTypeRepository.findAll(); }

    private BillingRunResponse toResponse(BillingRun run) {
        return BillingRunResponse.builder()
                .id(run.getId())
                .runNumber(run.getRunNumber())
                .billingPeriodStart(run.getBillingPeriodStart())
                .billingPeriodEnd(run.getBillingPeriodEnd())
                .billingScheduleId(run.getBillingSchedule() != null ? run.getBillingSchedule().getId() : null)
                .billingScheduleName(run.getBillingSchedule() != null ? run.getBillingSchedule().getName() : "Ad-hoc Run")
                .runType(run.getRunType())
                .status(run.getStatus())
                .totalProperties(run.getTotalProperties())
                .totalAmount(run.getTotalAmount())
                .totalTax(run.getTotalTax())
                .autoSend(run.getAutoSend())
                .executedBy(run.getExecutedBy())
                .executedAt(run.getExecutedAt())
                .lines(run.getLines() != null ? run.getLines().stream().map(l ->
                        BillingRunResponse.BillingRunLineDto.builder()
                                .id(l.getId())
                                .propertyId(l.getPropertyId())
                                .residentId(l.getResidentId())
                                .billingRuleId(l.getBillingRule() != null ? l.getBillingRule().getId() : null)
                                .billingRuleName(l.getBillingRule() != null ? l.getBillingRule().getName() : "")
                                .chargeTypeCode(l.getChargeType() != null ? l.getChargeType().getCode() : "")
                                .chargeTypeName(l.getChargeType() != null ? l.getChargeType().getName() : "")
                                .description(l.getDescription())
                                .quantity(l.getQuantity())
                                .rate(l.getRate())
                                .amount(l.getAmount())
                                .taxAmount(l.getTaxAmount())
                                .totalAmount(l.getTotalAmount())
                                .calculationDetails(l.getCalculationDetails())
                                .build()
                ).collect(Collectors.toList()) : List.of())
                .build();
    }
}
