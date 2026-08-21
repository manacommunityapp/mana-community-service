package com.manacommunity.api.notification.service;

import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.dto.SmsMessageResponse;

public interface SmsService {

    /** Persists the message and dispatches it asynchronously. Returns the saved SmsMessage id. */
    Long send(SendSmsRequest request);

    /** Manual retry of a failed/DLQ message by admin. */
    SmsMessageResponse retry(Long smsMessageId);

    /** Admin cancel — marks the message CANCELLED if not yet sent. */
    void cancel(Long smsMessageId);
}
