package com.manacommunity.api.billing.event;

import java.math.BigDecimal;

public record InvoicePaymentReceivedEvent(
        Long invoiceId,
        Long communityId,
        Long residentId,
        BigDecimal paidAmount,
        String invoiceNumber,
        String paymentReference
) {}
