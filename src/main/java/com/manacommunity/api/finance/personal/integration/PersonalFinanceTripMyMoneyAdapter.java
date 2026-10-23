package com.manacommunity.api.finance.personal.integration;

import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.trip.split.port.TripMyMoneyPort;
import com.manacommunity.api.trip.split.support.Money;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalFinanceTripMyMoneyAdapter implements TripMyMoneyPort {

    private final PersonalFinanceService personalFinanceService;
    private final AppUserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(Long userId, String sourceType, String sourceId, boolean income, long amountPaise,
                       LocalDate date, String label) {
        if (userId == null) {
            return;
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot sync trip transaction to My Money: user {} not found", userId);
            return;
        }

        BigDecimal amount = Money.toRupees(amountPaise);
        String type = income ? "INCOME" : "EXPENSE";
        personalFinanceService.upsertProjectionBySource(
                user,
                type,
                amount,
                date,
                "TRIPS",
                sourceType,
                sourceId,
                label,
                "Trips & Commute",
                "car-outline",
                "#0EA5E9"
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(Long userId, String sourceType, String sourceId) {
        if (userId == null) {
            return;
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        personalFinanceService.voidProjectionBySource(user, "TRIPS", sourceType, sourceId);
    }
}
