package com.manacommunity.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorSanitizerTest {

    @Test
    @DisplayName("Should scrub database table names from error messages")
    void testScrubTableNames() {
        String raw = "Record not found in tbl_users or cfbos_billing_rule";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("tbl_users");
        assertThat(sanitized).doesNotContain("cfbos_billing_rule");
        assertThat(sanitized).contains("resource");
    }

    @Test
    @DisplayName("Should scrub database constraints and [db: ...] hints")
    void testScrubConstraintsAndHints() {
        String raw = "Unique constraint violation [db: uq_users_phone_number] for uq_reg_event";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("[db:");
        assertThat(sanitized).doesNotContain("uq_users_phone_number");
        assertThat(sanitized).doesNotContain("uq_reg_event");
        assertThat(sanitized).doesNotContain("users");
    }

    @Test
    @DisplayName("Should replace raw SQL statements with safe generic message")
    void testScrubRawSql() {
        String raw = "Error executing select * from tbl_users where id = 5";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("select");
        assertThat(sanitized).doesNotContain("tbl_users");
        assertThat(sanitized).isEqualTo("A data integrity conflict occurred.");
    }

    @Test
    @DisplayName("Should replace database driver duplicate key errors without leaking tables or values")
    void testScrubDbDriverDuplicateKeyErrors() {
        String raw = "Duplicate entry '9876543210' for key 'tbl_residents.uq_phone'";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("9876543210");
        assertThat(sanitized).doesNotContain("tbl_residents");
        assertThat(sanitized).doesNotContain("uq_phone");
        assertThat(sanitized).isEqualTo("A record with this information already exists.");
    }

    @Test
    @DisplayName("Should replace database driver foreign key errors without leaking table relations")
    void testScrubDbDriverForeignKeyErrors() {
        String raw = "ERROR: insert or update on table \"order_items\" violates foreign key constraint \"fk_orders_user\"";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("order_items");
        assertThat(sanitized).doesNotContain("fk_orders_user");
        assertThat(sanitized).isEqualTo("The requested operation conflicts with related data or a required reference is missing.");
    }

    @Test
    @DisplayName("Should redact phone numbers from error messages")
    void testRedactPhoneNumbers() {
        String raw1 = "Invalid phone number: +91 9876543210 submitted";
        String raw2 = "Failed to dispatch SMS to 9876543210";

        assertThat(ErrorSanitizer.sanitize(raw1)).doesNotContain("9876543210");
        assertThat(ErrorSanitizer.sanitize(raw1)).contains("[REDACTED_PHONE]");

        assertThat(ErrorSanitizer.sanitize(raw2)).doesNotContain("9876543210");
        assertThat(ErrorSanitizer.sanitize(raw2)).contains("[REDACTED_PHONE]");
    }

    @Test
    @DisplayName("Should redact email addresses from error messages")
    void testRedactEmailAddresses() {
        String raw = "User with email sandeep.kumar@example.com is not allowed";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("sandeep.kumar@example.com");
        assertThat(sanitized).contains("[REDACTED_EMAIL]");
    }

    @Test
    @DisplayName("Should redact credit card numbers and Aadhaar IDs")
    void testRedactCardAndAadhaar() {
        String rawCard = "Payment failed for card 4111 2222 3333 4444";
        String rawAadhaar = "Verification failed for Aadhaar 1234 5678 9012";

        assertThat(ErrorSanitizer.sanitize(rawCard)).doesNotContain("4111 2222 3333 4444");
        assertThat(ErrorSanitizer.sanitize(rawCard)).contains("[REDACTED_CARD]");

        assertThat(ErrorSanitizer.sanitize(rawAadhaar)).doesNotContain("1234 5678 9012");
        assertThat(ErrorSanitizer.sanitize(rawAadhaar)).contains("[REDACTED_ID]");
    }

    @Test
    @DisplayName("Should redact secrets, passwords, and JWT tokens")
    void testRedactSecretsAndJwts() {
        String raw = "Auth failed for token=secretPassword123 with bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.xyz";
        String sanitized = ErrorSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("secretPassword123");
        assertThat(sanitized).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(sanitized).contains("token=[REDACTED]");
        assertThat(sanitized).contains("[REDACTED_TOKEN]");
    }

    @Test
    @DisplayName("Should clean table names into user-friendly singular entity names")
    void testCleanResourceName() {
        assertThat(ErrorSanitizer.cleanResourceName("tbl_users")).isEqualTo("User");
        assertThat(ErrorSanitizer.cleanResourceName("tbl_property_tenants")).isEqualTo("Property Tenant");
        assertThat(ErrorSanitizer.cleanResourceName("cfbos_ledger_entries")).isEqualTo("Ledger Entry");
        assertThat(ErrorSanitizer.cleanResourceName("communities")).isEqualTo("Community");
        assertThat(ErrorSanitizer.cleanResourceName(null)).isEqualTo("Record");
    }

    @Test
    @DisplayName("Should clean column prefixes from field names")
    void testCleanFieldName() {
        assertThat(ErrorSanitizer.cleanFieldName("col_phone_number")).isEqualTo("phone number");
        assertThat(ErrorSanitizer.cleanFieldName("fld_email")).isEqualTo("email");
        assertThat(ErrorSanitizer.cleanFieldName("title")).isEqualTo("title");
    }

    @Test
    @DisplayName("Should sanitize field errors properly")
    void testSanitizeFieldErrors() {
        List<ErrorResponse.FieldError> fieldErrors = List.of(
                ErrorResponse.FieldError.builder()
                        .field("col_user_phone")
                        .message("Invalid phone +91 9876543210")
                        .build()
        );

        List<ErrorResponse.FieldError> sanitized = ErrorSanitizer.sanitizeFieldErrors(fieldErrors);

        assertThat(sanitized).hasSize(1);
        assertThat(sanitized.get(0).getField()).isEqualTo("user phone");
        assertThat(sanitized.get(0).getMessage()).doesNotContain("9876543210");
        assertThat(sanitized.get(0).getMessage()).contains("[REDACTED_PHONE]");
    }

    @Test
    @DisplayName("Should leave normal safe messages intact")
    void testNormalMessagesIntact() {
        String normal = "Registration for 'Football Cup' is currently closed.";
        assertThat(ErrorSanitizer.sanitize(normal)).isEqualTo(normal);
    }
}
