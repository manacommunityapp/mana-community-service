package com.manacommunity.api.notification.enums;

public enum SmsLanguage {
    EN("English"),
    TE("Telugu"),
    HI("Hindi"),
    TA("Tamil"),
    KN("Kannada"),
    ML("Malayalam");

    public final String displayName;
    SmsLanguage(String displayName) { this.displayName = displayName; }
}
