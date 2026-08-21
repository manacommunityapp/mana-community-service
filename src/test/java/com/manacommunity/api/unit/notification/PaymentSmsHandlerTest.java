package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.notification.event.DonationSuccessEvent;
import com.manacommunity.api.notification.event.PaymentCapturedEvent;
import com.manacommunity.api.notification.handler.PaymentSmsHandler;
import com.manacommunity.api.notification.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSmsHandlerTest {

    @Mock private SmsService smsService;

    private PaymentSmsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentSmsHandler(smsService);
    }

    @Test
    @DisplayName("onPaymentCaptured sends PAYMENT_SUCCESSFUL with all required variables")
    void paymentCapturedSendsCorrectTemplate() {
        PaymentCapturedEvent event = new PaymentCapturedEvent(
                1L, "+919876543210", "Arun Kumar", new BigDecimal("500.00"), "ORD-001", "PAY-001");

        handler.onPaymentCaptured(event);

        verify(smsService).send(argThat(req ->
                "PAYMENT_SUCCESSFUL".equals(req.getTemplateCode())
                && "Arun Kumar".equals(req.getVariables().get("name"))
                && "500.00".equals(req.getVariables().get("amount"))
                && "PAY-001".equals(req.getVariables().get("paymentId"))
                && "ORD-001".equals(req.getVariables().get("orderId"))
        ));
    }

    @Test
    @DisplayName("onDonationSuccess sends DONATION_RECEIVED with receipt details")
    void donationSuccessSendsCorrectTemplate() {
        DonationSuccessEvent event = new DonationSuccessEvent(
                2L, "+919876543211", "Priya S", new BigDecimal("1000.00"), "REC-2024-001");

        handler.onDonationSuccess(event);

        verify(smsService).send(argThat(req ->
                "DONATION_RECEIVED".equals(req.getTemplateCode())
                && "Priya S".equals(req.getVariables().get("name"))
                && "1000.00".equals(req.getVariables().get("amount"))
                && "REC-2024-001".equals(req.getVariables().get("receiptId"))
        ));
    }

    @Test
    @DisplayName("exception from smsService is swallowed on payment event")
    void paymentExceptionSwallowed() {
        when(smsService.send(any())).thenThrow(new ManaCommunityException(
                "Provider down", HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR"));

        PaymentCapturedEvent event = new PaymentCapturedEvent(
                1L, "+919876543210", "Test", new BigDecimal("100"), "ORD", "PAY");

        assertThatCode(() -> handler.onPaymentCaptured(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("exception from smsService is swallowed on donation event")
    void donationExceptionSwallowed() {
        when(smsService.send(any())).thenThrow(new ManaCommunityException(
                "Provider down", HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR"));

        DonationSuccessEvent event = new DonationSuccessEvent(
                2L, "+919876543211", "Test", new BigDecimal("100"), "REC");

        assertThatCode(() -> handler.onDonationSuccess(event))
                .doesNotThrowAnyException();
    }
}
