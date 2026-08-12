package com.manacommunity.api.email;

/**
 * The set of transactional emails this application sends — the full Sports
 * Tournament email flow. Each value maps to a Thymeleaf template under
 * {@code classpath:/templates/email/} and a default subject line.
 */
public enum EmailTemplate {

    /** Player just submitted a registration (status REGISTERED). */
    REGISTRATION_RECEIVED("registration-received", "We received your registration", EmailCategory.REGISTRATION,
            new Trigger("Sports → Register", true,
                    "A player submits a registration for an event that requires admin approval; the entry lands PENDING.")),

    /** A registration was confirmed by the organiser (status CONFIRMED). */
    REGISTRATION_CONFIRMED("registration-confirmed", "You're confirmed!", EmailCategory.REGISTRATION,
            new Trigger("Sports → Register · Sports → Admin → Registrations", true,
                    "The registration auto-confirms (event doesn't require approval), or an admin confirms a PENDING entry.")),

    /** A registration was not approved (status REJECTED / WITHDRAWN by organiser). */
    REGISTRATION_REJECTED("registration-rejected", "Update on your registration", EmailCategory.REGISTRATION,
            new Trigger("Sports → Admin → Registrations", true,
                    "An admin rejects a pending registration, optionally with a reason.")),

    /** A tournament's match schedule was published. */
    SCHEDULE_PUBLISHED("schedule-published", "Match schedule is live", EmailCategory.TOURNAMENT,
            new Trigger("Sports → Schedule", true,
                    "An admin publishes the match schedule/fixtures for a tournament; confirmed participants are notified.")),

    /** A participant's own match is about to start. */
    MATCH_REMINDER("match-reminder", "Your match starts soon", EmailCategory.MATCH,
            new Trigger("Sports → Auction (team rosters) · Sports → Schedule (match time)", true,
                    "Automatic background job — fires shortly before a published match's scheduled start, to that match's two team rosters (owner, captain, players).")),

    /** A player won their match / advanced a round. */
    WINNER_NOTIFICATION("winner-notification", "You won your match! 🎉", EmailCategory.MATCH,
            new Trigger("Sports → Schedule (bracket / match result entry)", true,
                    "An admin records a completed match result; the winning side's players are emailed automatically.")),

    /** The tournament finished — champion, runner-up and third place. */
    TOURNAMENT_COMPLETION("tournament-completion", "Tournament results are in", EmailCategory.TOURNAMENT,
            new Trigger("Sports → Schedule (bracket / match result entry)", true,
                    "Fires automatically to all confirmed participants once the FINAL round's result is recorded (champion, runner-up, third place).")),

    /** Prize distribution / closing ceremony details. */
    PRIZE_DISTRIBUTION("prize-distribution", "Prize distribution details", EmailCategory.PRIZE,
            new Trigger(null, false,
                    "Not currently wired to any UI action or automatic job — defined for a future prize-ceremony flow. Reachable only from this admin test-send tool.")),

    /** Tournament created — announcing registration is open. */
    TOURNAMENT_OPEN("tournament-open", "Registration is now open!", EmailCategory.TOURNAMENT,
            new Trigger("Sports → Admin → Tournament management → \"Notify Registration Open\"", true,
                    "An admin clicks \"Notify Registration Open\" for a tournament (or picks this template in the Announcement composer).")),

    /** General tournament announcement (custom or pre-built). */
    TOURNAMENT_ANNOUNCEMENT("tournament-announcement", "Tournament announcement", EmailCategory.ANNOUNCEMENT,
            new Trigger("Sports → Admin → Tournament management → Announcement", true,
                    "Default template for the admin's free-form tournament announcement composer (venue changes, general updates, etc.).")),

    /** One-time passcode emailed to verify an address before registration. */
    EMAIL_OTP("email-otp", "Your verification code", EmailCategory.AUTH,
            new Trigger("Sports → Register (event registration form)", true,
                    "A player requests an email verification code before submitting a sports event registration.")),

    /** Tournament registrations are now open for the community. */
    REGISTRATION_OPEN("registration-open", "Registrations are now open!", EmailCategory.REGISTRATION,
            new Trigger("Sports → Admin → Tournament management → Update Status", true,
                    "Sent automatically to all participants when an admin changes a tournament's status to \"Registration Open.\"")),

    /** Tournament has officially started / opening ceremony. */
    TOURNAMENT_START("tournament-start", "Welcome to the tournament! 🚀", EmailCategory.TOURNAMENT,
            new Trigger(null, false,
                    "Not currently wired to any UI action or automatic job — defined for a future opening-ceremony flow. Reachable only from this admin test-send tool.")),

    /** Major community event or festival announcement. */
    EVENT_ANNOUNCEMENT("event-announcement", "Community Event Announcement 🎆", EmailCategory.EVENT,
            new Trigger("Events → Announcements", true,
                    "Fires when a major community event or festival is announced to all members.")),

    /** Upcoming event reminder to registered participants. */
    EVENT_REMINDER("event-reminder", "Upcoming Event Reminder ⏰", EmailCategory.EVENT,
            new Trigger("Events → Schedule", true,
                    "Sent before an upcoming community event to remind attendees of pending actions or timing.")),

    /** Event fundraising / festival contribution appeal. */
    EVENT_DONATION_APPEAL("event-donation-appeal", "Support Our Festival & Event Drive 🚩", EmailCategory.EVENT,
            new Trigger("Events → Fundraising", true,
                    "Fires for donation appeals, festival fundraising, or community contribution drives.")),

    /** Event RSVP / registration confirmation. */
    EVENT_REGISTRATION_CONFIRMED("event-registration-confirmed", "RSVP Confirmed for Event 🎉", EmailCategory.EVENT,
            new Trigger("Events → Registration", true,
                    "Sent immediately after a member registers/RSVPs for a community event.")),

    /** Invite members to volunteer for event operations & committees. */
    EVENT_VOLUNTEER_INVITATION("event-volunteer-invitation", "Join as a Community Volunteer 🤝", EmailCategory.EVENT,
            new Trigger("Events → Operations & Volunteers", true,
                    "Invites members to volunteer for committee responsibilities and event setups.")),

    /** Post-event thank-you note with media links & highlights. */
    EVENT_THANK_YOU("event-thank-you", "Thank You for Joining Us! ❤️", EmailCategory.EVENT,
            new Trigger("Events → Media & Reports", true,
                    "Post-event thank-you email sent to participants with media highlights and highlights reel.")),

    /** Schedule, venue, or itinerary update for an event. */
    EVENT_SCHEDULE_UPDATE("event-schedule-update", "Important Schedule Update 📅", EmailCategory.EVENT,
            new Trigger("Events → Schedule", true,
                    "Notifies registered members of venue, timing, or itinerary changes for an event.")),

    /** Event cancellation or postponement notification. */
    EVENT_CANCELLATION("event-cancellation", "Event Cancellation Notice ⚠️", EmailCategory.EVENT,
            new Trigger("Events → Management", true,
                    "Fires if a community event is cancelled or postponed due to weather/unforeseen reasons."));

    private final String templateName;
    private final String defaultSubject;
    private final EmailCategory category;
    private final Trigger trigger;

    EmailTemplate(String templateName, String defaultSubject, EmailCategory category, Trigger trigger) {
        this.templateName = templateName;
        this.defaultSubject = defaultSubject;
        this.category = category;
        this.trigger = trigger;
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

    /** Where/how this email actually gets sent in the live app — surfaced in admin tooling. */
    public Trigger trigger() {
        return trigger;
    }

    public enum EmailCategory {
        REGISTRATION, TOURNAMENT, MATCH, PRIZE, ANNOUNCEMENT, AUTH, EVENT
    }

    /**
     * @param menuPath human-readable module/menu/submenu path where this fires, or
     *                 {@code null} when the template isn't wired to any live trigger yet
     * @param wired    false when nothing in the app currently calls this template
     * @param description what specifically causes the send
     */
    public record Trigger(String menuPath, boolean wired, String description) {}
}
