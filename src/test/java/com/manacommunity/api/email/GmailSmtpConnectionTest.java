package com.manacommunity.api.email;

import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Manual connectivity test for Gmail SMTP.
 * Run locally with your App Password to verify email delivery works.
 *
 * Usage:
 *   1. Set env vars GMAIL_USERNAME and GMAIL_APP_PASSWORD
 *   2. Remove @Disabled from the test you want to run
 *   3. Run: mvn test -pl . -Dtest=GmailSmtpConnectionTest -Dspring.profiles.active=local
 *
 * Or run directly:
 *   GMAIL_USERNAME=manacommunityapp@gmail.com \
 *   GMAIL_APP_PASSWORD=your-16-char-app-password \
 *   mvn test -pl . -Dtest=GmailSmtpConnectionTest#testSmtpConnection
 */
class GmailSmtpConnectionTest {

    private static final String USERNAME = System.getenv("GMAIL_USERNAME") != null
            ? System.getenv("GMAIL_USERNAME") : "manacommunityapp@gmail.com";
    private static final String PASSWORD = System.getenv("GMAIL_APP_PASSWORD");

    private Properties smtpProps() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return props;
    }

    @Test
    @Disabled("Enable manually — requires GMAIL_APP_PASSWORD env var")
    void testSmtpConnection() throws Exception {
        assertPassword();

        Session session = Session.getInstance(smtpProps());
        try (Transport transport = session.getTransport("smtp")) {
            System.out.println("Connecting to smtp.gmail.com:587 ...");
            transport.connect("smtp.gmail.com", 587, USERNAME, PASSWORD);
            System.out.println("SUCCESS — SMTP connection established and authenticated.");
            System.out.println("  Username: " + USERNAME);
            System.out.println("  Connected: " + transport.isConnected());
        }
    }

    @Test
    @Disabled("Enable manually — requires GMAIL_APP_PASSWORD env var")
    void testSendEmail() throws Exception {
        assertPassword();

        Session session = Session.getInstance(smtpProps());
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(USERNAME, "Mana Community (Test)"));
        msg.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(USERNAME));
        msg.setSubject("[TEST] Mana Community — Gmail SMTP Verification");
        msg.setContent(
                "<div style='font-family:sans-serif;padding:24px;'>"
                + "<h2 style='color:#059669;'>Gmail SMTP is working!</h2>"
                + "<p>This test email confirms that your local development environment "
                + "can send emails through Gmail's SMTP relay.</p>"
                + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:20px 0'/>"
                + "<p style='color:#6b7280;font-size:12px;'>Sent from GmailSmtpConnectionTest</p>"
                + "</div>",
                "text/html; charset=UTF-8"
        );

        System.out.println("Sending test email to " + USERNAME + " ...");
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect("smtp.gmail.com", 587, USERNAME, PASSWORD);
            transport.sendMessage(msg, msg.getAllRecipients());
        }
        System.out.println("SUCCESS — test email sent. Check your inbox.");
    }

    private void assertPassword() {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "GMAIL_APP_PASSWORD env var is not set.\n"
                    + "Generate one at: https://myaccount.google.com/apppasswords\n"
                    + "Then run: GMAIL_APP_PASSWORD=xxxx mvn test -Dtest=GmailSmtpConnectionTest#testSmtpConnection"
            );
        }
    }
}
