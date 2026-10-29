package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a maintenance invoice or dues are generated.
 * Triggers a finance push notification to the resident.
 */
@Getter
public class MaintenanceInvoiceEvent extends ApplicationEvent {

    private final Long residentId;
    private final String flatNumber;
    private final double amount;
    private final String dueDate;
    private final Long invoiceId;

    public MaintenanceInvoiceEvent(Object source,
                                   Long residentId,
                                   String flatNumber,
                                   double amount,
                                   String dueDate,
                                   Long invoiceId) {
        super(source);
        this.residentId = residentId;
        this.flatNumber = flatNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.invoiceId = invoiceId;
    }
}