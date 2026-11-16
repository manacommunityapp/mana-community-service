package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrLeaveBalance;
import com.manacommunity.api.hrm.entity.HrLeaveType;
import com.manacommunity.api.hrm.repository.HrLeaveBalanceRepository;
import com.manacommunity.api.hrm.repository.HrLeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrLeaveCarryForwardService {

    private final HrLeaveBalanceRepository leaveBalanceRepository;
    private final HrLeaveTypeRepository leaveTypeRepository;

    // Runs at midnight on Jan 1 each year
    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void processYearEndCarryForward() {
        int previousYear = LocalDate.now().getYear() - 1;
        int newYear = LocalDate.now().getYear();
        log.info("Processing leave carry-forward from {} to {}", previousYear, newYear);

        List<HrLeaveType> carryForwardTypes = leaveTypeRepository.findByIsCarryForwardTrue();

        for (HrLeaveType leaveType : carryForwardTypes) {
            List<HrLeaveBalance> previousBalances = leaveBalanceRepository
                    .findByLeaveTypeIdAndYear(leaveType.getId(), previousYear);

            for (HrLeaveBalance prevBalance : previousBalances) {
                double carryAmount = prevBalance.getRemainingBalance();
                if (leaveType.getMaxCarryForward() != null) {
                    carryAmount = Math.min(carryAmount, leaveType.getMaxCarryForward());
                }

                if (carryAmount <= 0) continue;

                HrLeaveBalance newBalance = leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeIdAndYear(
                                prevBalance.getEmployee().getId(), leaveType.getId(), newYear)
                        .orElseGet(() -> HrLeaveBalance.builder()
                                .organizationId(prevBalance.getOrganizationId())
                                .employee(prevBalance.getEmployee())
                                .leaveType(leaveType)
                                .year(newYear)
                                .totalBalance((double) leaveType.getDefaultBalance())
                                .usedBalance(0.0)
                                .remainingBalance((double) leaveType.getDefaultBalance())
                                .build());

                newBalance.setTotalBalance(newBalance.getTotalBalance() + carryAmount);
                newBalance.setRemainingBalance(newBalance.getRemainingBalance() + carryAmount);
                leaveBalanceRepository.save(newBalance);
                log.info("Carried forward {} days for employee {} leave type {}",
                        carryAmount, prevBalance.getEmployee().getId(), leaveType.getId());
            }
        }
        log.info("Leave carry-forward processing complete for year {}", newYear);
    }
}
