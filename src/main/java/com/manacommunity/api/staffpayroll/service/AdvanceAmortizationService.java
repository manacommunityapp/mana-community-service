package com.manacommunity.api.staffpayroll.service;

import com.manacommunity.api.staffpayroll.entity.AdvanceInstallment;
import com.manacommunity.api.staffpayroll.entity.AdvanceInstallment.InstallmentStatus;
import com.manacommunity.api.staffpayroll.entity.EmployerContract;
import com.manacommunity.api.staffpayroll.entity.SalaryAdvance;
import com.manacommunity.api.staffpayroll.entity.SalaryAdvance.*;
import com.manacommunity.api.staffpayroll.repository.AdvanceInstallmentRepository;
import com.manacommunity.api.staffpayroll.repository.EmployerContractRepository;
import com.manacommunity.api.staffpayroll.repository.SalaryAdvanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvanceAmortizationService {

    private final SalaryAdvanceRepository advanceRepo;
    private final AdvanceInstallmentRepository installmentRepo;
    private final EmployerContractRepository contractRepo;

    @Transactional
    public SalaryAdvance disburseAdvance(Long staffId, String staffName, Long contractId,
                                           double amount, int installments, PaymentMethod method,
                                           String upiTxnId, String purpose) {
        EmployerContract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        double emi = Math.round(amount / installments * 100.0) / 100.0;

        SalaryAdvance advance = SalaryAdvance.builder()
                .communityId(contract.getCommunityId())
                .staffId(staffId).staffName(staffName).contractId(contractId)
                .employerFlatNumber(contract.getEmployerFlatNumber())
                .employerName(contract.getEmployerName())
                .principalAmount(amount).totalInstallments(installments)
                .emiAmount(emi).installmentsPaid(0)
                .amountRepaid(0).outstandingBalance(amount)
                .status(AdvanceStatus.ACTIVE).paymentMethod(method)
                .upiTransactionId(upiTxnId).disbursedAt(LocalDateTime.now())
                .firstEmiDate(getNextCycleDate(contract))
                .purpose(purpose)
                .build();

        advance = advanceRepo.save(advance);

        // Generate installment schedule
        List<AdvanceInstallment> schedule = new ArrayList<>();
        LocalDate emiDate = advance.getFirstEmiDate();
        double remaining = amount;

        for (int i = 1; i <= installments; i++) {
            double installmentAmt = (i == installments) ? remaining : emi;
            remaining -= installmentAmt;

            schedule.add(AdvanceInstallment.builder()
                    .advanceId(advance.getId()).staffId(staffId).contractId(contractId)
                    .installmentNumber(i).amount(installmentAmt)
                    .dueDate(emiDate)
                    .payrollMonth(String.format("%d-%02d", emiDate.getYear(), emiDate.getMonthValue()))
                    .status(InstallmentStatus.SCHEDULED)
                    .balanceAfterDeduction(Math.max(0, amount - (emi * i)))
                    .build());

            emiDate = emiDate.plusMonths(1);
        }

        installmentRepo.saveAll(schedule);
        advance.setLastEmiDate(emiDate.minusMonths(1));
        return advanceRepo.save(advance);
    }

    @Transactional
    public AdvanceInstallment deductInstallment(Long advanceId, int installmentNumber) {
        SalaryAdvance advance = advanceRepo.findById(advanceId)
                .orElseThrow(() -> new RuntimeException("Advance not found"));

        List<AdvanceInstallment> installments = installmentRepo.findByAdvanceIdOrderByInstallmentNumber(advanceId);
        AdvanceInstallment inst = installments.stream()
                .filter(i -> i.getInstallmentNumber() == installmentNumber)
                .findFirst().orElseThrow(() -> new RuntimeException("Installment not found"));

        inst.setStatus(InstallmentStatus.DEDUCTED);
        inst.setDeductedAt(LocalDateTime.now());

        advance.setInstallmentsPaid(advance.getInstallmentsPaid() + 1);
        advance.setAmountRepaid(advance.getAmountRepaid() + inst.getAmount());
        advance.setOutstandingBalance(advance.getPrincipalAmount() - advance.getAmountRepaid());

        if (advance.getInstallmentsPaid() >= advance.getTotalInstallments()) {
            advance.setStatus(AdvanceStatus.FULLY_REPAID);
            advance.setOutstandingBalance(0);
        }

        advanceRepo.save(advance);
        return installmentRepo.save(inst);
    }

    @Transactional
    public SalaryAdvance handleMidMonthTermination(Long advanceId) {
        SalaryAdvance advance = advanceRepo.findById(advanceId)
                .orElseThrow(() -> new RuntimeException("Advance not found"));

        advance.setTerminatedEarly(true);
        advance.setTerminationDate(LocalDate.now());
        advance.setSettlementAmount(advance.getOutstandingBalance());
        advance.setStatus(AdvanceStatus.TERMINATED_PENDING);

        // Mark remaining installments as skipped
        List<AdvanceInstallment> remaining = installmentRepo
                .findByAdvanceIdAndStatus(advanceId, InstallmentStatus.SCHEDULED);
        remaining.forEach(inst -> {
            inst.setStatus(InstallmentStatus.SKIPPED);
            inst.setRemarks("Contract terminated — settlement pending");
        });
        installmentRepo.saveAll(remaining);

        return advanceRepo.save(advance);
    }

    public List<AdvanceInstallment> getAmortizationSchedule(Long advanceId) {
        return installmentRepo.findByAdvanceIdOrderByInstallmentNumber(advanceId);
    }

    public AdvanceDashboard getAdvanceDashboard() {
        List<SalaryAdvance> all = advanceRepo.findAll();
        List<AdvanceInstallment> allInst = installmentRepo.findAll();

        double totalDisbursed = all.stream().mapToDouble(SalaryAdvance::getPrincipalAmount).sum();
        double totalRepaid = all.stream().mapToDouble(SalaryAdvance::getAmountRepaid).sum();
        double totalOutstanding = all.stream()
                .filter(a -> a.getStatus() == AdvanceStatus.ACTIVE)
                .mapToDouble(SalaryAdvance::getOutstandingBalance).sum();
        long activeCount = all.stream().filter(a -> a.getStatus() == AdvanceStatus.ACTIVE).count();
        long terminatedPending = all.stream().filter(a -> a.getStatus() == AdvanceStatus.TERMINATED_PENDING).count();

        return new AdvanceDashboard(all.size(), activeCount, terminatedPending,
                totalDisbursed, totalRepaid, totalOutstanding,
                all, allInst);
    }

    private LocalDate getNextCycleDate(EmployerContract contract) {
        LocalDate now = LocalDate.now();
        int cycleDay = switch (contract.getSalaryCycle()) {
            case TENTH_TO_TENTH -> 10;
            case FIFTEENTH_TO_FIFTEENTH -> 15;
            default -> 1;
        };
        LocalDate next = LocalDate.of(now.getYear(), now.getMonthValue(), cycleDay);
        return next.isAfter(now) ? next : next.plusMonths(1);
    }

    public record AdvanceDashboard(int totalAdvances, long activeAdvances, long terminatedPending,
                                     double totalDisbursed, double totalRepaid, double totalOutstanding,
                                     List<SalaryAdvance> advances, List<AdvanceInstallment> installments) {}
}
