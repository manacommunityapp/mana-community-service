package com.manacommunity.api.notification.template;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.notification.entity.SmsTemplate;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.repository.SmsTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves a template by code and language, with automatic EN fallback.
 * Also handles rendering via SmsTemplateEngine.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsTemplateResolver {

    private final SmsTemplateRepository templateRepository;
    private final SmsTemplateEngine engine;

    public String resolve(String templateCode, SmsLanguage language, Map<String, String> variables) {
        SmsTemplate template = findTemplate(templateCode, language);
        return engine.render(template.getBody(), variables);
    }

    public SmsTemplate findTemplate(String templateCode, SmsLanguage language) {
        return templateRepository.findActiveByCodeAndLanguage(templateCode, language)
                .or(() -> {
                    if (language != SmsLanguage.EN) {
                        log.debug("Template {} not found for {}, falling back to EN", templateCode, language);
                        return templateRepository.findActiveByCodeAndLanguage(templateCode, SmsLanguage.EN);
                    }
                    return java.util.Optional.empty();
                })
                .orElseThrow(() -> new ManaCommunityException(
                        "SMS template not found: " + templateCode, HttpStatus.NOT_FOUND, "SMS_TEMPLATE_NOT_FOUND"));
    }
}
