package com.manacommunity.api.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class DonationSuccessEvent {
    private final Long userId;
    private final String phoneNumber;
    private final String fullName;
    private final BigDecimal amount;
    private final String receiptId;
}
