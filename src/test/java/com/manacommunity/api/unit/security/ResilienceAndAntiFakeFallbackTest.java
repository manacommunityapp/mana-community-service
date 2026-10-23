package com.manacommunity.api.unit.security;

import com.manacommunity.api.exception.*;
import com.manacommunity.api.security.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResilienceAndAntiFakeFallbackTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ResilienceAuditService resilienceAuditService;

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("WalletUnavailableException returns 503 and correct error code")
    void walletUnavailableException_returns503() {
        WalletUnavailableException ex = new WalletUnavailableException("Wallet ledger is temporarily unreachable.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getErrorCode()).isEqualTo("WALLET_SERVICE_UNAVAILABLE");
        assertThat(ex.getSubsystem()).isEqualTo("WALLET");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cfbos/wallet/123");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleServiceDegraded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("WALLET_SERVICE_UNAVAILABLE");
        assertThat(response.getBody().getMessage()).contains("Wallet ledger is temporarily unreachable.");
    }

    @Test
    @DisplayName("PaymentGatewayUnavailableException returns 503 and correct error code")
    void paymentGatewayUnavailableException_returns503() {
        PaymentGatewayUnavailableException ex = new PaymentGatewayUnavailableException("Razorpay gateway timeout.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getErrorCode()).isEqualTo("PAYMENT_GATEWAY_UNAVAILABLE");
        assertThat(ex.getSubsystem()).isEqualTo("PAYMENT_GATEWAY");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/cfbos/payments/charge");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleServiceDegraded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("PAYMENT_GATEWAY_UNAVAILABLE");
    }

    @Test
    @DisplayName("EmergencyDispatchFailedException returns 503 and correct error code")
    void emergencyDispatchFailedException_returns503() {
        EmergencyDispatchFailedException ex = new EmergencyDispatchFailedException("Failed to notify security guards.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getErrorCode()).isEqualTo("EMERGENCY_DISPATCH_FAILED");
        assertThat(ex.getSubsystem()).isEqualTo("EMERGENCY");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/emergency/sos");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleServiceDegraded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("EMERGENCY_DISPATCH_FAILED");
    }

    @Test
    @DisplayName("ResilienceAuditService logs circuit breaker trip and retry exhaustion")
    void resilienceAuditService_logsEvents() {
        resilienceAuditService.recordFailure(
                AuditModule.PAYMENTS,
                AuditAction.CIRCUIT_BREAKER_TRIP,
                "PaymentGateway",
                "ORD-999",
                "504 Gateway Timeout"
        );

        verify(auditService).record(
                AuditAction.CIRCUIT_BREAKER_TRIP,
                AuditModule.PAYMENTS,
                "PaymentGateway",
                "ORD-999",
                null,
                "FAILED: 504 Gateway Timeout"
        );

        resilienceAuditService.recordRetryExhausted(
                AuditModule.WALLET,
                "LedgerDebit",
                "TXN-101",
                3,
                "Database lock timeout"
        );

        verify(auditService).record(
                AuditAction.RETRY_EXHAUSTED,
                AuditModule.WALLET,
                "LedgerDebit",
                "TXN-101",
                null,
                "Attempts: 3 | Final Error: Database lock timeout"
        );
    }
}
