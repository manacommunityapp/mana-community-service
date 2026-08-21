package com.manacommunity.api.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegistrationConfirmedEvent {
    private final Long userId;
    private final String phoneNumber;
    private final String fullName;
    private final String communityName;
}
