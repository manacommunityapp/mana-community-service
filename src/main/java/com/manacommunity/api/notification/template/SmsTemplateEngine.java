package com.manacommunity.api.notification.template;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple {{variable}} substitution engine — no SpEL, no arbitrary code execution.
 * Unknown variables are left as-is so callers can detect them.
 */
@Component
public class SmsTemplateEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) return "";
        if (variables == null || variables.isEmpty()) return template;

        StringBuffer sb = new StringBuffer();
        Matcher m = VARIABLE_PATTERN.matcher(template);
        while (m.find()) {
            String key = m.group(1);
            String replacement = variables.getOrDefault(key, m.group(0));  // keep original if missing
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Returns true when no {{variable}} placeholders remain in the rendered body. */
    public boolean isFullyRendered(String rendered) {
        return rendered != null && !VARIABLE_PATTERN.matcher(rendered).find();
    }
}
