package com.manacommunity.api.email;

import lombok.RequiredArgsConstructor;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renders an {@link EmailTemplate} to an HTML string using the auto-configured
 * Thymeleaf engine (templates live under {@code classpath:/templates/email/}).
 * Injected as {@link ITemplateEngine} so it is agnostic to the Spring-integration
 * artifact version pulled in by the Boot starter.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final ITemplateEngine templateEngine;

    /**
     * @param template the email to render
     * @param variables model attributes referenced by the template (e.g. {@code playerName})
     * @return the rendered HTML body
     */
    public String render(EmailTemplate template, Map<String, Object> variables) {
        Context ctx = new Context();
        if (variables != null) {
            ctx.setVariables(variables);
        }
        return templateEngine.process(template.templateName(), ctx);
    }
}
