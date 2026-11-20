package com.manacommunity.api.communityops.pass;

public record DigitalPassRequest(
    String flatNumber,
    PassType passType,
    String visitorName,
    String vehicleNumber,
    String purpose
) {}
