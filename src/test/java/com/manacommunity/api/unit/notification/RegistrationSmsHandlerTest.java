package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.notification.event.RegistrationApprovedEvent;
import com.manacommunity.api.notification.event.RegistrationConfirmedEvent;
import com.manacommunity.api.notification.event.RegistrationRejectedEvent;
import com.manacommunity.api.notification.handler.RegistrationSmsHandler;
import com.manacommunity.api.notification.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationSmsHandlerTest {

    @Mock private SmsService smsService;

    private RegistrationSmsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegistrationSmsHandler(smsService);
    }

    @Test
    @DisplayName("onRegistrationConfirmed sends REGISTRATION_CONFIRMED template with name and community")
    void confirmedSendsCorrectTemplate() {
        RegistrationConfirmedEvent event = new RegistrationConfirmedEvent(
                1L, "+919876543210", "Arun Kumar", "ManaLocal");

        handler.onRegistrationConfirmed(event);

        verify(smsService).send(argThat(req ->
                "REGISTRATION_CONFIRMED".equals(req.getTemplateCode())
                && "Arun Kumar".equals(req.getVariables().get("name"))
                && "ManaLocal".equals(req.getVariables().get("community"))
                && "+919876543210".equals(req.getPhoneNumber())
        ));
    }

    @Test
    @DisplayName("onRegistrationApproved sends REGISTRATION_APPROVED template")
    void approvedSendsCorrectTemplate() {
        RegistrationApprovedEvent event = new RegistrationApprovedEvent(
                2L, "+919876543211", "Priya S", "ManaLocal");

        handler.onRegistrationApproved(event);

        verify(smsService).send(argThat(req ->
                "REGISTRATION_APPROVED".equals(req.getTemplateCode())
                && "Priya S".equals(req.getVariables().get("name"))
        ));
    }

    @Test
    @DisplayName("onRegistrationRejected sends REGISTRATION_REJECTED with reason variable")
    void rejectedSendsCorrectTemplate() {
        RegistrationRejectedEvent event = new RegistrationRejectedEvent(
                3L, "+919876543212", "Kiran B", "Incomplete documents");

        handler.onRegistrationRejected(event);

        verify(smsService).send(argThat(req ->
                "REGISTRATION_REJECTED".equals(req.getTemplateCode())
                && "Incomplete documents".equals(req.getVariables().get("reason"))
        ));
    }

    @Test
    @DisplayName("exception from smsService is swallowed — handler does not rethrow")
    void exceptionSwallowed() {
        when(smsService.send(any())).thenThrow(new ManaCommunityException(
                "Provider down", HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR"));

        RegistrationConfirmedEvent event = new RegistrationConfirmedEvent(
                1L, "+919876543210", "Test User", "Community");

        assertThatCode(() -> handler.onRegistrationConfirmed(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null reason in rejection event is handled without NPE")
    void nullReasonHandled() {
        RegistrationRejectedEvent event = new RegistrationRejectedEvent(
                4L, "+919876543213", "Anita R", null);

        assertThatCode(() -> handler.onRegistrationRejected(event))
                .doesNotThrowAnyException();
    }
}
