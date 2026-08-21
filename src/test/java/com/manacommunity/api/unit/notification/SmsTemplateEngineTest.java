package com.manacommunity.api.unit.notification;

import com.manacommunity.api.notification.template.SmsTemplateEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmsTemplateEngineTest {

    private SmsTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SmsTemplateEngine();
    }

    @Test
    @DisplayName("renders all variables in a template")
    void rendersVariables() {
        String body = "Hello {{name}}, your OTP is {{otp}} (expires in {{expiry}} min).";
        String result = engine.render(body, Map.of("name", "Arun", "otp", "123456", "expiry", "5"));
        assertThat(result).isEqualTo("Hello Arun, your OTP is 123456 (expires in 5 min).");
    }

    @Test
    @DisplayName("leaves unknown variables as-is")
    void unknownVariablesRetained() {
        String body = "Hi {{name}}, event: {{eventName}}";
        String result = engine.render(body, Map.of("name", "Priya"));
        assertThat(result).contains("{{eventName}}");
        assertThat(result).contains("Priya");
    }

    @Test
    @DisplayName("returns template unchanged when variable map is empty")
    void emptyVariablesRetained() {
        String body = "Hello {{name}}";
        assertThat(engine.render(body, Map.of())).isEqualTo(body);
    }

    @Test
    @DisplayName("null template returns empty string")
    void nullTemplate() {
        assertThat(engine.render(null, Map.of("a", "b"))).isEmpty();
    }

    @Test
    @DisplayName("isFullyRendered detects remaining placeholders")
    void isFullyRendered() {
        assertThat(engine.isFullyRendered("Hello Arun")).isTrue();
        assertThat(engine.isFullyRendered("Hello {{name}}")).isFalse();
    }

    @Test
    @DisplayName("handles special regex characters in replacement values")
    void specialCharsInValue() {
        String body = "Amount: {{amount}}";
        String result = engine.render(body, Map.of("amount", "₹1,500.00"));
        assertThat(result).isEqualTo("Amount: ₹1,500.00");
    }
}
