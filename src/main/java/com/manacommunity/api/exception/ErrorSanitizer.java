package com.manacommunity.api.exception;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sanitizes all exception messages and validation error details returned to API clients.
 *
 * <p>Strictly enforces security and data privacy:
 * <ul>
 *   <li>Never exposes database table names (e.g. {@code tbl_users}, {@code cfbos_ledger}).</li>
 *   <li>Never exposes database constraint names (e.g. {@code uq_users_email}, {@code fk_order_user}).</li>
 *   <li>Never exposes raw SQL statements (e.g. {@code SELECT}, {@code INSERT INTO}).</li>
 *   <li>Never exposes raw database driver messages or SQLSTATE codes.</li>
 *   <li>Never exposes personal identifiable information (PII) including phone numbers, email addresses,
 *       credit card numbers, national IDs, passwords, secrets, and JWT tokens.</li>
 *   <li>Never exposes internal Java package/class paths (e.g. {@code com.manacommunity...}).</li>
 * </ul>
 */
public final class ErrorSanitizer {

    private ErrorSanitizer() {}

    // ─── 1. PII Regex Patterns ───────────────────────────────────────────────

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}|\\b(?:\\+?91[-.\\s]?)?[6-9]\\d{9}\\b");

    private static final Pattern CARD_PATTERN = Pattern.compile(
            "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b");

    private static final Pattern AADHAAR_PATTERN = Pattern.compile(
            "\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b");

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(password|secret|bearer|token|pin|otp|access_key|secret_key|api_key)\\s*[=:]\\s*['\"]?([^\\s,'\"]+)");

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");

    // ─── 2. Database & Schema Leak Patterns ───────────────────────────────────

    private static final Pattern DB_HINT_PATTERN = Pattern.compile(
            "(?i)\\[db:\\s*[^\\]]+\\]");

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "(?i)\\b(?:tbl_|t_|cfbos_|sports_|billing_|sys_|act_)[a-zA-Z0-9_]+\\b");

    private static final Pattern CONSTRAINT_NAME_PATTERN = Pattern.compile(
            "(?i)\\b(?:uq_|fk_|pk_|idx_|uk_)[a-zA-Z0-9_]+\\b");

    private static final Pattern SQL_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(select\\s+.+\\s+from|insert\\s+into|update\\s+.+\\s+set|delete\\s+from|alter\\s+table|drop\\s+table)\\b");

    private static final Pattern DB_DRIVER_ERROR_PATTERN = Pattern.compile(
            "(?i)(?:Unique index or primary key violation|Duplicate entry|violates foreign key constraint|violates not-null constraint|column \".*\" does not exist|relation \".*\" does not exist|SQLSTATE|org\\.hibernate|org\\.postgresql|com\\.mysql|org\\.h2)");

    private static final Pattern CLASS_PACKAGE_PATTERN = Pattern.compile(
            "\\b(?:com\\.manacommunity|org\\.springframework|jakarta\\.)[a-zA-Z0-9_.]+\\b");

    /**
     * Sanitizes a client-facing exception message by removing all PII, database table names,
     * constraint names, and SQL details.
     *
     * @param rawMessage the raw exception message
     * @return clean, safe, human-friendly message
     */
    public static String sanitize(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "An unexpected error occurred.";
        }

        // If message contains raw SQL or database driver error, replace entirely with safe copy
        if (SQL_KEYWORD_PATTERN.matcher(rawMessage).find() || DB_DRIVER_ERROR_PATTERN.matcher(rawMessage).find()) {
            String lower = rawMessage.toLowerCase();
            if (lower.contains("duplicate") || lower.contains("unique") || lower.contains("already exists")) {
                return "A record with this information already exists.";
            }
            if (lower.contains("foreign key") || lower.contains("references") || lower.contains("not-null")) {
                return "The requested operation conflicts with related data or a required reference is missing.";
            }
            return "A data integrity conflict occurred.";
        }

        String msg = rawMessage;

        // Strip [db: ...] constraint hints
        msg = DB_HINT_PATTERN.matcher(msg).replaceAll("").trim();

        // Redact table names and constraint identifiers
        msg = TABLE_NAME_PATTERN.matcher(msg).replaceAll("resource");
        msg = CONSTRAINT_NAME_PATTERN.matcher(msg).replaceAll("data constraint");

        // Redact sensitive secrets and tokens
        msg = SECRET_PATTERN.matcher(msg).replaceAll("$1=[REDACTED]");
        msg = JWT_PATTERN.matcher(msg).replaceAll("[REDACTED_TOKEN]");
        msg = CARD_PATTERN.matcher(msg).replaceAll("[REDACTED_CARD]");
        msg = AADHAAR_PATTERN.matcher(msg).replaceAll("[REDACTED_ID]");

        // Redact PII (email, phone)
        msg = EMAIL_PATTERN.matcher(msg).replaceAll("[REDACTED_EMAIL]");
        msg = PHONE_PATTERN.matcher(msg).replaceAll("[REDACTED_PHONE]");

        // Sanitize internal package/class references
        msg = CLASS_PACKAGE_PATTERN.matcher(msg).replaceAll("SystemComponent");

        return msg.trim().replaceAll("\\s{2,}", " ");
    }

    /**
     * Sanitizes a list of field validation errors.
     *
     * @param fieldErrors original field errors
     * @return sanitized field errors with stripped PII and safe field names
     */
    public static List<ErrorResponse.FieldError> sanitizeFieldErrors(List<ErrorResponse.FieldError> fieldErrors) {
        if (fieldErrors == null) return null;
        return fieldErrors.stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(cleanFieldName(fe.getField()))
                        .message(sanitize(fe.getMessage()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Cleans raw database column/field names (e.g. col_user_phone -> user_phone).
     */
    public static String cleanFieldName(String field) {
        if (field == null) return null;
        return field.replaceAll("(?i)^col_|^fld_", "").replace('_', ' ').trim();
    }

    /**
     * Cleans raw database table names into user-friendly singular names
     * (e.g. tbl_resident_profiles -> Resident Profile).
     */
    public static String cleanResourceName(String resource) {
        if (resource == null || resource.isBlank()) return "Record";
        String cleaned = resource.replaceAll("(?i)^tbl_|^t_|^cfbos_|^sports_|^billing_|^sys_|^act_|^mana_", "")
                .replace('_', ' ').trim();
        if (cleaned.endsWith("ies") && cleaned.length() > 4) {
            cleaned = cleaned.substring(0, cleaned.length() - 3) + "y";
        } else if (cleaned.endsWith("s") && cleaned.length() > 3 && !cleaned.toLowerCase().endsWith("ss")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return sb.toString();
    }
}
