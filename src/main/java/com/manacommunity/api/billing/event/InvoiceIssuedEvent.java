package com.manacommunity.api.billing.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceIssuedEvent(
        Long invoiceId,
        Long communityId,
        Long residentId,
        BigDecimal amount,
        String invoiceNumber,
        LocalDate dueDate
) {}
