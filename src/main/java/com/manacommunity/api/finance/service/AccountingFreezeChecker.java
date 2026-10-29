package com.manacommunity.api.finance.service;

import com.manacommunity.api.repository.CommunitySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AccountingFreezeChecker {

    private final CommunitySettingsRepository communitySettingsRepository;

    public void assertNotFrozen(Long communityId, LocalDate voucherDate) {
        communitySettingsRepository.findByCommunityId(communityId).ifPresent(settings -> {
            LocalDate freezeDate = settings.getAccountingFreezeDate();
            if (freezeDate != null && !voucherDate.isAfter(freezeDate)) {
                throw new IllegalStateException(
                        "Period is frozen. No changes allowed before " + freezeDate);
            }
        });
    }
}
