package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a visitor checks in at the gate or requires approval.
 * Triggers a high-priority security push notification on the resident's mobile device.
 */
@Getter
public class VisitorArrivalEvent extends ApplicationEvent {

    private final Long residentId;
    private final String visitorName;
    private final String flatNumber;
    private final String purpose;
    private final Long passId;

    public VisitorArrivalEvent(Object source,
                               Long residentId,
                               String visitorName,
                               String flatNumber,
                               String purpose,
                               Long passId) {
        super(source);
        this.residentId = residentId;
        this.visitorName = visitorName;
        this.flatNumber = flatNumber;
        this.purpose = purpose;
        this.passId = passId;
    }
}