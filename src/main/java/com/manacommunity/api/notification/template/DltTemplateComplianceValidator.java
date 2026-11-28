package com.manacommunity.api.notification.template;

import com.manacommunity.api.notification.entity.SmsTemplate;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.TemplateStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates India TRAI DLT / Meta utility template registration, per-variable
 * character caps (default 30 chars per DLT variable slot), and Indic Unicode /
 * UCS-2 encoding compliance before SMS or WhatsApp dispatch.
 */
@Component
public class DltTemplateComplianceValidator {

    public static final int DEFAULT_MAX_VARIABLE_CHARS = 30;
    public static final int GSM7_SINGLE_SEGMENT_LIMIT = 160;
    public static final int UCS2_SINGLE_SEGMENT_LIMIT = 70;

    private static final Pattern DLT_NUMERIC_ID = Pattern.compile("^\\d{16,19}$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public record ValidationResult(
            boolean compliant,
            List<String> violations,
            String renderedBody,
            boolean containsIndicScript,
            int smsSegmentCount
    ) {}

    public ValidationResult validateForDispatch(
            SmsTemplate template,
            String dltEntityId,
            Map<String, String> variables,
            int maxVariableChars
    ) {
        List<String> violations = new ArrayList<>();
        int varLimit = maxVariableChars > 0 ? maxVariableChars : DEFAULT_MAX_VARIABLE_CHARS;

        if (template == null) {
            return new ValidationResult(false, List.of("Template is null"), "", false, 0);
        }

        // 1. DLT Entity ID & Template ID registration check
        if (dltEntityId == null || !DLT_NUMERIC_ID.matcher(dltEntityId.trim()).matches()) {
            violations.add("Invalid or missing TRAI DLT Entity ID (must be 16-19 digits)");
        }
        if (template.getDltTemplateId() == null || !DLT_NUMERIC_ID.matcher(template.getDltTemplateId().trim()).matches()) {
            violations.add("Invalid or missing DLT Template ID for " + template.getTemplateCode());
        }
        if (template.getStatus() != TemplateStatus.ACTIVE && template.getStatus() != TemplateStatus.APPROVED) {
            violations.add("Template status is " + template.getStatus() + "; must be APPROVED or ACTIVE");
        }

        // 2. Variable presence & per-variable DLT char limit check (prevents Meta/DLT carrier drops)
        Map<String, String> safeVars = variables != null ? variables : Map.of();
        String body = template.getBody() != null ? template.getBody() : "";
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = safeVars.get(key);
            if (val == null) {
                violations.add("Missing value for template variable {{" + key + "}}");
            } else if (val.length() > varLimit) {
                violations.add(String.format(
                        "Variable {{%s}} length %d exceeds TRAI DLT %d-char limit", key, val.length(), varLimit));
            }
        }

        String rendered = renderSimple(body, safeVars);

        // 3. Regional Indic script / Unicode flag & max length check
        boolean hasNonAscii = containsNonAscii(rendered);
        if (hasNonAscii && !template.isUnicode()) {
            violations.add("Regional Indic script detected for language " + template.getLanguage()
                    + " but unicode=false (causes GSM-7 carrier garbling/drop)");
        }
        if (template.getLanguage() != null && template.getLanguage() != SmsLanguage.EN && hasNonAscii && !template.isUnicode()) {
            // Already captured above; ensure segment math still reflects UCS-2
        }

        int segmentLimit = (hasNonAscii || template.isUnicode()) ? UCS2_SINGLE_SEGMENT_LIMIT : GSM7_SINGLE_SEGMENT_LIMIT;
        int segments = rendered.isEmpty() ? 0 : (int) Math.ceil((double) rendered.length() / segmentLimit);

        if (template.getMaxLength() != null && rendered.length() > template.getMaxLength()) {
            violations.add(String.format(
                    "Rendered message length %d exceeds template maxLength %d", rendered.length(), template.getMaxLength()));
        }

        return new ValidationResult(violations.isEmpty(), List.copyOf(violations), rendered, hasNonAscii, segments);
    }

    private String renderSimple(String template, Map<String, String> vars) {
        StringBuffer sb = new StringBuffer();
        Matcher m = VARIABLE_PATTERN.matcher(template);
        while (m.find()) {
            String key = m.group(1);
            String replacement = vars.getOrDefault(key, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean containsNonAscii(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }
}
