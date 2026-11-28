package com.manacommunity.api.iot.edge.service;

import com.manacommunity.api.iot.edge.entity.PrepaidMeterAccount;
import com.manacommunity.api.iot.edge.repository.PrepaidMeterAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrepaidMeterDeductionService {

    private final PrepaidMeterAccountRepository accountRepository;

    private static final int MAX_RETRIES = 3;

    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
               maxAttempts = MAX_RETRIES,
               backoff = @Backoff(delay = 50, multiplier = 2))
    public DeductionResult processTelemetryReading(Long meterId, long sequenceNumber,
                                                    BigDecimal deltaConsumption, Instant readingTimestamp) {
        PrepaidMeterAccount account = accountRepository.findByMeterIdForUpdate(meterId)
                .orElseThrow(() -> new IllegalArgumentException("No prepaid account for meter: " + meterId));

        if (!account.getActive()) {
            return new DeductionResult(meterId, false, "Account inactive", BigDecimal.ZERO, account.getBalance());
        }

        if (sequenceNumber <= account.getLastSequenceNumber()) {
            log.debug("Out-of-order packet for meter {}: seq {} <= last {}", meterId, sequenceNumber, account.getLastSequenceNumber());
            return new DeductionResult(meterId, false, "Duplicate/out-of-order packet (seq " + sequenceNumber + ")",
                    BigDecimal.ZERO, account.getBalance());
        }

        if (deltaConsumption.compareTo(BigDecimal.ZERO) <= 0) {
            account.setLastSequenceNumber(sequenceNumber);
            accountRepository.save(account);
            return new DeductionResult(meterId, true, "Zero consumption", BigDecimal.ZERO, account.getBalance());
        }

        BigDecimal cost = deltaConsumption.multiply(account.getRatePerUnit()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal newBalance = account.getBalance().subtract(cost);

        account.setBalance(newBalance);
        account.setLastSequenceNumber(sequenceNumber);
        account.setTotalConsumed(account.getTotalConsumed().add(deltaConsumption));
        account.setLastDeductionAt(Instant.now());

        boolean lowBalanceTriggered = false;
        if (newBalance.compareTo(account.getLowBalanceThreshold()) <= 0 && !account.getLowBalanceAlertSent()) {
            account.setLowBalanceAlertSent(true);
            lowBalanceTriggered = true;
            log.warn("Low balance alert for meter {}: balance={}, threshold={}", meterId, newBalance, account.getLowBalanceThreshold());
        }

        boolean disconnectTriggered = false;
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0 && !account.getIsDisconnected()) {
            account.setIsDisconnected(true);
            disconnectTriggered = true;
            log.warn("Balance exhausted for meter {}: disconnecting supply", meterId);
        }

        accountRepository.save(account);

        String message = "Deducted " + cost + " for " + deltaConsumption + " units";
        if (lowBalanceTriggered) message += " [LOW BALANCE ALERT]";
        if (disconnectTriggered) message += " [DISCONNECTED]";

        return new DeductionResult(meterId, true, message, cost, newBalance,
                lowBalanceTriggered, disconnectTriggered);
    }

    @Transactional
    public PrepaidMeterAccount rechargeAccount(Long meterId, BigDecimal amount) {
        PrepaidMeterAccount account = accountRepository.findByMeterId(meterId)
                .orElseThrow(() -> new IllegalArgumentException("No prepaid account for meter: " + meterId));

        account.setBalance(account.getBalance().add(amount));

        if (account.getBalance().compareTo(account.getLowBalanceThreshold()) > 0) {
            account.setLowBalanceAlertSent(false);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            account.setIsDisconnected(false);
        }

        return accountRepository.save(account);
    }

    public List<PrepaidMeterAccount> getAccountsByCommunity(Long communityId) {
        return accountRepository.findByCommunityId(communityId);
    }

    public List<LowBalanceAlert> checkLowBalanceAlerts() {
        List<PrepaidMeterAccount> accounts = accountRepository.findAccountsNeedingLowBalanceAlert();
        List<LowBalanceAlert> alerts = new ArrayList<>();
        for (PrepaidMeterAccount account : accounts) {
            alerts.add(new LowBalanceAlert(account.getMeterId(), account.getCommunityId(),
                    account.getUnitNumber(), account.getBalance(), account.getLowBalanceThreshold()));
        }
        return alerts;
    }

    public record DeductionResult(Long meterId, boolean success, String message,
                                   BigDecimal amountDeducted, BigDecimal remainingBalance,
                                   boolean lowBalanceAlert, boolean disconnected) {
        public DeductionResult(Long meterId, boolean success, String message,
                               BigDecimal amountDeducted, BigDecimal remainingBalance) {
            this(meterId, success, message, amountDeducted, remainingBalance, false, false);
        }
    }

    public record LowBalanceAlert(Long meterId, Long communityId, String unitNumber,
                                   BigDecimal balance, BigDecimal threshold) {}
}
