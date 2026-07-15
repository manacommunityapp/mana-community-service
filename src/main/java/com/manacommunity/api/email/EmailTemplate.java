package com.manacommunity.api.email;

/**
 * The set of transactional emails this application sends — the full Sports
 * Tournament email flow. Each value maps to a Thymeleaf template under
 * {@code classpath:/templates/email/} and a default subject line.
 */
public enum EmailTemplate {

    /** Player just submitted a registration (status REGISTERED). */
    REGISTRATION_RECEIVED("registration-received", "We received your registration"),

    /** A registration was confirmed by the organiser (status CONFIRMED). */
    REGISTRATION_CONFIRMED("registration-confirmed", "You're confirmed!"),

    /** A registration was not approved (status REJECTED / WITHDRAWN by organiser). */
    REGISTRATION_REJECTED("registration-rejected", "Update on your registration"),

    /** A tournament's match schedule was published. */
    SCHEDULE_PUBLISHED("schedule-published", "Match schedule is live"),

    /** A participant's own match is about to start. */
    MATCH_REMINDER("match-reminder", "Your match starts soon"),

    /** A player won their match / advanced a round. */
    WINNER_NOTIFICATION("winner-notification", "You won your match! 🎉"),

    /** The tournament finished — champion, runner-up and third place. */
    TOURNAMENT_COMPLETION("tournament-completion", "Tournament results are in"),

    /** Prize distribution / closing ceremony details. */
    PRIZE_DISTRIBUTION("prize-distribution", "Prize distribution details"),

    /** One-time passcode emailed to verify an address before registration. */
    EMAIL_OTP("email-otp", "Your verification code"),

    /** Tournament registrations are now open for the community. */
    REGISTRATION_OPEN("registration-open", "Registrations are now open!"),

    /** Tournament has officially started / opening ceremony. */
    TOURNAMENT_START("tournament-start", "Welcome to the tournament! 🚀");

    private final String templateName;
    private final String defaultSubject;

    EmailTemplate(String templateName, String defaultSubject) {
        this.templateName = templateName;
        this.defaultSubject = defaultSubject;
    }

    /** Thymeleaf logical name, e.g. {@code email/registration-received}. */
    public String templateName() {
        return "email/" + templateName;
    }

    public String defaultSubject() {
        return defaultSubject;
    }
}
