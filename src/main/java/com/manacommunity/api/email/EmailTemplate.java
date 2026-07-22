package com.manacommunity.api.email;

/**
 * The set of transactional emails this application sends — the full Sports
 * Tournament email flow. Each value maps to a Thymeleaf template under
 * {@code classpath:/templates/email/} and a default subject line.
 */
public enum EmailTemplate {

    /** Player just submitted a registration (status REGISTERED). */
    REGISTRATION_RECEIVED("registration-received", "We received your registration", EmailCategory.REGISTRATION),

    /** A registration was confirmed by the organiser (status CONFIRMED). */
    REGISTRATION_CONFIRMED("registration-confirmed", "You're confirmed!", EmailCategory.REGISTRATION),

    /** A registration was not approved (status REJECTED / WITHDRAWN by organiser). */
    REGISTRATION_REJECTED("registration-rejected", "Update on your registration", EmailCategory.REGISTRATION),

    /** A tournament's match schedule was published. */
    SCHEDULE_PUBLISHED("schedule-published", "Match schedule is live", EmailCategory.TOURNAMENT),

    /** A participant's own match is about to start. */
    MATCH_REMINDER("match-reminder", "Your match starts soon", EmailCategory.MATCH),

    /** A player won their match / advanced a round. */
    WINNER_NOTIFICATION("winner-notification", "You won your match! 🎉", EmailCategory.MATCH),

    /** The tournament finished — champion, runner-up and third place. */
    TOURNAMENT_COMPLETION("tournament-completion", "Tournament results are in", EmailCategory.TOURNAMENT),

    /** Prize distribution / closing ceremony details. */
    PRIZE_DISTRIBUTION("prize-distribution", "Prize distribution details", EmailCategory.PRIZE),

    /** Tournament created — announcing registration is open. */
    TOURNAMENT_OPEN("tournament-open", "Registration is now open!", EmailCategory.TOURNAMENT),

    /** General tournament announcement (custom or pre-built). */
    TOURNAMENT_ANNOUNCEMENT("tournament-announcement", "Tournament announcement", EmailCategory.ANNOUNCEMENT),

    /** One-time passcode emailed to verify an address before registration. */
    EMAIL_OTP("email-otp", "Your verification code", EmailCategory.AUTH),

    /** Tournament registrations are now open for the community. */
    REGISTRATION_OPEN("registration-open", "Registrations are now open!", EmailCategory.REGISTRATION),

    /** Tournament has officially started / opening ceremony. */
    TOURNAMENT_START("tournament-start", "Welcome to the tournament! 🚀", EmailCategory.TOURNAMENT);

    private final String templateName;
    private final String defaultSubject;
    private final EmailCategory category;

    EmailTemplate(String templateName, String defaultSubject, EmailCategory category) {
        this.templateName = templateName;
        this.defaultSubject = defaultSubject;
        this.category = category;
    }

    /** Thymeleaf logical name, e.g. {@code email/registration-received}. */
    public String templateName() {
        return "email/" + templateName;
    }

    public String defaultSubject() {
        return defaultSubject;
    }

    /**
     * Broad grouping used by admin tooling to style/order templates without needing
     * a per-template lookup table that has to be updated by hand every time a new
     * template is added.
     */
    public EmailCategory category() {
        return category;
    }

    public enum EmailCategory {
        REGISTRATION, TOURNAMENT, MATCH, PRIZE, ANNOUNCEMENT, AUTH
    }
}
