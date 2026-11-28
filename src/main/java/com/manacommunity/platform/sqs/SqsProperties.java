package com.manacommunity.platform.sqs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS SQS async decoupling ({@code app.sqs.*}).
 *
 * <p>{@code enabled} defaults to {@code false} so local/dev/test environments
 * automatically execute in-process fallbacks without requiring AWS SQS queues.</p>
 */
@Data
@ConfigurationProperties(prefix = "app.sqs")
public class SqsProperties {

    private boolean enabled = false;
    private boolean autoCreateQueues = true;
    private String region = "eu-north-1";
    private String accessKey = "";
    private String secretKey = "";
    private String endpoint = "";
    private long pollDelayMs = 2000L;
    private int maxMessagesPerPoll = 10;
    private int waitTimeSeconds = 10;
    private Queues queues = new Queues();

    @Data
    public static class Queues {
        private String smsDispatch = "mana-sms-dispatch";
        private String emailDispatch = "mana-email-dispatch";
        private String notificationEvents = "mana-notification-events";
        private String sportsEvents = "mana-sports-events";
        private String auctionNotifications = "mana-auction-notifications";
        private String iotAlerts = "mana-iot-alerts";
        private String mobilityEvents = "mana-mobility-events";
        private String groupOrderNotifications = "mana-grouporder-notifications";
        private String externalSync = "mana-external-sync";
        private String reportJobs = "mana-report-jobs";
        private String paymentEvents = "mana-payment-events";
        private String emergencySos = "mana-emergency-sos";
    }
}
