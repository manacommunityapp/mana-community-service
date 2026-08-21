package com.manacommunity.api.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private boolean enabled = false;
    private String provider = "TWILIO";          // TWILIO | MSG91 | AWS_SNS | MOCK
    private String defaultCountryCode = "+91";
    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String whatsappFrom;
    private Retry retry = new Retry();
    private Otp otp = new Otp();
    private RateLimit rateLimit = new RateLimit();
    private List<String> testPhoneNumbers = List.of();  // always deliver to these (test/QA)

    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelaySeconds = 30;
        private long maxDelaySeconds = 600;
        private double backoffMultiplier = 4.0;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { maxAttempts = v; }
        public long getInitialDelaySeconds() { return initialDelaySeconds; }
        public void setInitialDelaySeconds(long v) { initialDelaySeconds = v; }
        public long getMaxDelaySeconds() { return maxDelaySeconds; }
        public void setMaxDelaySeconds(long v) { maxDelaySeconds = v; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double v) { backoffMultiplier = v; }
    }

    public static class Otp {
        private int length = 6;
        private int expiryMinutes = 5;
        private int maxAttempts = 5;
        private int maxSendsPerHour = 5;

        public int getLength() { return length; }
        public void setLength(int v) { length = v; }
        public int getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(int v) { expiryMinutes = v; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { maxAttempts = v; }
        public int getMaxSendsPerHour() { return maxSendsPerHour; }
        public void setMaxSendsPerHour(int v) { maxSendsPerHour = v; }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int perUserPerMinute = 2;
        private int perPhonePerMinute = 2;
        private int perIpPerMinute = 10;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { enabled = v; }
        public int getPerUserPerMinute() { return perUserPerMinute; }
        public void setPerUserPerMinute(int v) { perUserPerMinute = v; }
        public int getPerPhonePerMinute() { return perPhonePerMinute; }
        public void setPerPhonePerMinute(int v) { perPhonePerMinute = v; }
        public int getPerIpPerMinute() { return perIpPerMinute; }
        public void setPerIpPerMinute(int v) { perIpPerMinute = v; }
    }

    // ── getters / setters ────────────────────────────────────────────────────
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public String getProvider() { return provider; }
    public void setProvider(String v) { provider = v; }
    public String getDefaultCountryCode() { return defaultCountryCode; }
    public void setDefaultCountryCode(String v) { defaultCountryCode = v; }
    public String getAccountSid() { return accountSid; }
    public void setAccountSid(String v) { accountSid = v; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String v) { authToken = v; }
    public String getFromNumber() { return fromNumber; }
    public void setFromNumber(String v) { fromNumber = v; }
    public String getWhatsappFrom() { return whatsappFrom; }
    public void setWhatsappFrom(String v) { whatsappFrom = v; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry v) { retry = v; }
    public Otp getOtp() { return otp; }
    public void setOtp(Otp v) { otp = v; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit v) { rateLimit = v; }
    public List<String> getTestPhoneNumbers() { return testPhoneNumbers; }
    public void setTestPhoneNumbers(List<String> v) { testPhoneNumbers = v; }
}
