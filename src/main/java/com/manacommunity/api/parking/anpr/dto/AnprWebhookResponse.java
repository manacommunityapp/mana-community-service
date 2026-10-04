package com.manacommunity.api.parking.anpr.dto;

import com.manacommunity.api.parking.anpr.entity.AnprGateEvent;

import java.time.LocalDateTime;

/** Response returned from the webhook endpoint to the ANPR service. */
public record AnprWebhookResponse(
        Long eventId,
        String plateNumber,
        AnprGateEvent.BarrierAction barrierAction,
        String matchedResidentName,
        String message
) {}
