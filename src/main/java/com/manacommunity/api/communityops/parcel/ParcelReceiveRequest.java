package com.manacommunity.api.communityops.parcel;

public record ParcelReceiveRequest(
    String flatNumber,
    Long residentUserId,
    String senderName,
    String courierName,
    String trackingNumber,
    String description,
    String notes
) {}
