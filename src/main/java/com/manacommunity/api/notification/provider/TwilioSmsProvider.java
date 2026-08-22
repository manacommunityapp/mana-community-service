package com.manacommunity.api.notification.provider;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "TWILIO")
@Slf4j
public class TwilioSmsProvider implements SmsProvider {

    private final SmsProperties props;

    public TwilioSmsProvider(SmsProperties props) {
        this.props = props;
        if (props.getAccountSid() != null && props.getAuthToken() != null) {
            Twilio.init(props.getAccountSid(), props.getAuthToken());
            log.info("Twilio SMS provider initialised (accountSid={})", props.getAccountSid());
        }
    }

    @Override
    public SmsSendResponse send(SmsSendRequest request) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(request.getTo()),
                    new PhoneNumber(request.getFrom() != null ? request.getFrom() : props.getFromNumber()),
                    request.getBody()
            ).create();

            return SmsSendResponse.builder()
                    .success(true)
                    .providerMessageId(message.getSid())
                    .status(message.getStatus() != null ? message.getStatus().toString() : "queued")
                    .rawResponse(message.getSid())
                    .build();

        } catch (ApiException ex) {
            log.error("Twilio send failed to {}: [{}] {}", request.getTo(), ex.getCode(), ex.getMessage());
            return SmsSendResponse.builder()
                    .success(false)
                    .errorCode(String.valueOf(ex.getCode()))
                    .errorMessage(ex.getMessage())
                    .rawResponse(ex.getMessage())
                    .build();
        }
    }

    @Override
    public String getDeliveryStatus(String providerMessageId) {
        try {
            Message message = Message.fetcher(providerMessageId).fetch();
            return message.getStatus() != null ? message.getStatus().toString() : "unknown";
        } catch (ApiException ex) {
            log.warn("Could not fetch Twilio status for {}: {}", providerMessageId, ex.getMessage());
            return "unknown";
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // A lightweight balance check to verify credentials are valid
            com.twilio.rest.api.v2010.Account.fetcher(props.getAccountSid()).fetch();
            return true;
        } catch (Exception ex) {
            log.warn("Twilio health check failed: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "TWILIO";
    }
}
