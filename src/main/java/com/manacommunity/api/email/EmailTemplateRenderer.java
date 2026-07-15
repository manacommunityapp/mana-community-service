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
        String html = templateEngine.process(template.templateName(), ctx);

        // Post-process to inject dynamic banner and sponsor images
        if (variables != null) {
            if (variables.containsKey("bannerUrl") && variables.get("bannerUrl") != null) {
                String bannerUrl = String.valueOf(variables.get("bannerUrl")).trim();
                if (!bannerUrl.isEmpty()) {
                    String bannerHtml = "\n          <!-- Dynamic Banner -->\n          <tr>\n            <td style=\"padding:0;\">\n              <img src=\"" + bannerUrl + "\" alt=\"Banner\" style=\"width:100%;max-width:600px;display:block;height:auto;margin:0 auto;\" />\n            </td>\n          </tr>";
                    html = html.replaceFirst("</tr>", "</tr>" + bannerHtml);
                }
            }
            if (variables.containsKey("sponsorImageUrl") && variables.get("sponsorImageUrl") != null) {
                String sponsorUrl = String.valueOf(variables.get("sponsorImageUrl")).trim();
                if (!sponsorUrl.isEmpty()) {
                    String sponsorHtml = "<!-- Dynamic Sponsor -->\n          <tr>\n            <td style=\"padding:16px 32px;text-align:center;background-color:#f8fafc;border-top:1px solid #eef0f2;\">\n              <p style=\"margin:0;font-size:10px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:0.05em;\">Sponsored By</p>\n              <img src=\"" + sponsorUrl + "\" alt=\"Sponsor\" style=\"max-height:45px;max-width:200px;display:inline-block;height:auto;vertical-align:middle;margin-top:12px;\" />\n            </td>\n          </tr>\n          ";
                    html = html.replace("<!-- Footer -->", sponsorHtml + "<!-- Footer -->");
                }
            }
        }

        return html;
    }
}
