package com.manacommunity.api.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Standalone Gmail SMTP connection test.
 *
 * Mirrors the settings from application-local.yaml:
 *   spring.mail.host     = smtp.gmail.com
 *   spring.mail.port     = 587
 *   spring.mail.username = manacommunityapp@gmail.com
 *   spring.mail.password = <app-password>
 *   STARTTLS + AUTH      = true
 *
 * Run as a plain main() — no Spring context required.
 * Override credentials via env vars:
 *   GMAIL_USERNAME      (default: manacommunityapp@gmail.com)
 *   GMAIL_APP_PASSWORD  (default: rygzuwgwbsvckiju)
 *   GMAIL_TEST_TO       (default: same as username — sends to self)
 */
public class GmailSmtpConnectionLocalTest {

    // ── SMTP settings (matches application-local.yaml) ──────────────────────
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    // ── Credentials (env vars take priority, fallback to local defaults) ─────
    private static final String USERNAME = System.getenv().getOrDefault(
            "GMAIL_USERNAME", "manacommunityapp@gmail.com");
    private static final String PASSWORD = System.getenv().getOrDefault(
            "GMAIL_APP_PASSWORD", "rygzuwgwbsvckiju");

    // ── Test recipient (defaults to self so no external mail is sent) ────────
    private static final String TO = System.getenv().getOrDefault(
            "GMAIL_TEST_TO", USERNAME);

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Gmail SMTP Connection Test");
        System.out.println("=================================================");
        System.out.printf("  Host     : %s:%d%n", SMTP_HOST, SMTP_PORT);
        System.out.printf("  Username : %s%n", USERNAME);
        System.out.printf("  Password : %s%n", maskPassword(PASSWORD));
        System.out.printf("  Send To  : %s%n", TO);
        System.out.println("-------------------------------------------------");
        System.out.println("  TLS      : STARTTLS (port 587)");
        System.out.println("-------------------------------------------------");

        // Step 1 — Build SMTP Properties
        Properties props = buildSmtpProperties();

        // Step 2 — Create authenticated Session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        session.setDebug(false); // set true for full SMTP handshake trace

        // Step 3 — Attempt to send a test email
        try {
            System.out.println("[1/3] Creating MimeMessage...");
            MimeMessage message = buildTestMessage(session);

            System.out.println("[2/3] Connecting to SMTP server & sending...");
            Transport.send(message);

            System.out.println("[3/3] SUCCESS - Test email sent to: " + TO);
            System.out.println("=================================================");

        } catch (AuthenticationFailedException e) {
            System.err.println("[ERROR] AUTH FAILED - Check username/app-password.");
            System.err.println("   Ensure 2FA is enabled and the App Password is correct.");
            System.err.println("   Error: " + e.getMessage());
            System.exit(1);

        } catch (MessagingException e) {
            System.err.println("[ERROR] SMTP ERROR - Could not send mail.");
            System.err.println("   Cause: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   Root:  " + e.getCause().getMessage());
            }
            System.exit(1);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Properties buildSmtpProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host",                SMTP_HOST);
        props.put("mail.smtp.port",                String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.starttls.enable",     "true");
        props.put("mail.smtp.starttls.required",   "true");
        props.put("mail.smtp.connectiontimeout",   "5000");
        props.put("mail.smtp.timeout",             "5000");
        props.put("mail.smtp.writetimeout",        "5000");
        return props;
    }

    private static MimeMessage buildTestMessage(Session session) throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(USERNAME, "Mana Community (SMTP Test)", "UTF-8"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        msg.setSubject("[TEST] Gmail SMTP Test - Mana Community");
        msg.setText(
                "This is an automated test email from GmailSmtpConnectionTest.\n\n" +
                "If you received this, your Gmail SMTP configuration is working correctly.\n\n" +
                "Host     : " + SMTP_HOST + ":" + SMTP_PORT + "\n" +
                "Username : " + USERNAME + "\n" +
                "TLS      : STARTTLS (port 587)\n"
        );
        return msg;
    }

    private static String maskPassword(String password) {
        if (password == null || password.length() < 4) return "****";
        return password.substring(0, 2) + "*".repeat(password.length() - 4) + password.substring(password.length() - 2);
    }
}
