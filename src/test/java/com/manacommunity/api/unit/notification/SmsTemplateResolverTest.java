package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.notification.entity.SmsTemplate;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.TemplateStatus;
import com.manacommunity.api.notification.repository.SmsTemplateRepository;
import com.manacommunity.api.notification.template.SmsTemplateEngine;
import com.manacommunity.api.notification.template.SmsTemplateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsTemplateResolverTest {

    @Mock private SmsTemplateRepository templateRepo;

    private SmsTemplateEngine engine;
    private SmsTemplateResolver resolver;

    @BeforeEach
    void setUp() {
        engine = new SmsTemplateEngine();
        resolver = new SmsTemplateResolver(templateRepo, engine);
    }

    private SmsTemplate activeTemplate(String code, SmsLanguage lang, String body) {
        return SmsTemplate.builder()
                .templateCode(code).language(lang).name(code)
                .body(body).status(TemplateStatus.ACTIVE)
                .messageType(MessageType.TRANSACTIONAL).build();
    }

    @Test
    @DisplayName("exact language match returns rendered body")
    void exactLanguageMatch() {
        SmsTemplate tpl = activeTemplate("REG_CONFIRM", SmsLanguage.EN, "Welcome {{name}}!");
        when(templateRepo.findActiveByCodeAndLanguage("REG_CONFIRM", SmsLanguage.EN))
                .thenReturn(Optional.of(tpl));

        String result = resolver.resolve("REG_CONFIRM", SmsLanguage.EN, Map.of("name", "Ravi"));

        assertThat(result).isEqualTo("Welcome Ravi!");
        verify(templateRepo, times(1)).findActiveByCodeAndLanguage("REG_CONFIRM", SmsLanguage.EN);
    }

    @Test
    @DisplayName("non-EN language not found falls back to EN")
    void nonEnFallsBackToEn() {
        when(templateRepo.findActiveByCodeAndLanguage("REG_CONFIRM", SmsLanguage.TE))
                .thenReturn(Optional.empty());
        SmsTemplate enTpl = activeTemplate("REG_CONFIRM", SmsLanguage.EN, "Hello {{name}}");
        when(templateRepo.findActiveByCodeAndLanguage("REG_CONFIRM", SmsLanguage.EN))
                .thenReturn(Optional.of(enTpl));

        String result = resolver.resolve("REG_CONFIRM", SmsLanguage.TE, Map.of("name", "Kumar"));

        assertThat(result).isEqualTo("Hello Kumar");
        verify(templateRepo).findActiveByCodeAndLanguage("REG_CONFIRM", SmsLanguage.TE);
        verify(templateRepo).findActiveByCodeAndLanguage("REG_CONFIRM", SmsLanguage.EN);
    }

    @Test
    @DisplayName("neither requested language nor EN found → throws ManaCommunityException")
    void neitherLanguageFoundThrows() {
        when(templateRepo.findActiveByCodeAndLanguage("MISSING", SmsLanguage.HI))
                .thenReturn(Optional.empty());
        when(templateRepo.findActiveByCodeAndLanguage("MISSING", SmsLanguage.EN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("MISSING", SmsLanguage.HI, Map.of()))
                .isInstanceOf(ManaCommunityException.class)
                .hasMessageContaining("MISSING");
    }

    @Test
    @DisplayName("EN language missing template → throws without second lookup")
    void enLanguageMissingThrows() {
        when(templateRepo.findActiveByCodeAndLanguage("GONE", SmsLanguage.EN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("GONE", SmsLanguage.EN, Map.of()))
                .isInstanceOf(ManaCommunityException.class);
        // Only 1 DB call — no redundant fallback to EN when already requesting EN
        verify(templateRepo, times(1)).findActiveByCodeAndLanguage(any(), any());
    }

    @Test
    @DisplayName("variables are substituted via SmsTemplateEngine")
    void variablesSubstituted() {
        SmsTemplate tpl = activeTemplate("PAYMENT", SmsLanguage.EN,
                "Payment of {{amount}} for order {{orderId}} confirmed.");
        when(templateRepo.findActiveByCodeAndLanguage("PAYMENT", SmsLanguage.EN))
                .thenReturn(Optional.of(tpl));

        String result = resolver.resolve("PAYMENT", SmsLanguage.EN,
                Map.of("amount", "500", "orderId", "ORD-001"));

        assertThat(result).isEqualTo("Payment of 500 for order ORD-001 confirmed.");
    }

    @Test
    @DisplayName("null variables map renders template without substitution")
    void nullVariablesRendersTemplate() {
        SmsTemplate tpl = activeTemplate("SIMPLE", SmsLanguage.EN, "Hello user!");
        when(templateRepo.findActiveByCodeAndLanguage("SIMPLE", SmsLanguage.EN))
                .thenReturn(Optional.of(tpl));

        String result = resolver.resolve("SIMPLE", SmsLanguage.EN, null);

        assertThat(result).isEqualTo("Hello user!");
    }
}
