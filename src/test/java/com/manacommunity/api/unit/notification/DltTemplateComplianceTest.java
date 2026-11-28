package com.manacommunity.api.unit.notification;

import com.manacommunity.api.notification.entity.SmsTemplate;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.TemplateStatus;
import com.manacommunity.api.notification.template.DltTemplateComplianceValidator;
import com.manacommunity.api.notification.template.DltTemplateComplianceValidator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DltTemplateComplianceTest {

    private final DltTemplateComplianceValidator validator = new DltTemplateComplianceValidator();
    private static final String VALID_ENTITY_ID = "1101456789012345";
    private static final String VALID_TEMPLATE_ID = "1107167890123456";

    @Test
    void passesCompliantEnglishDltTemplate() {
        SmsTemplate tpl = SmsTemplate.builder()
                .templateCode("OTP_LOGIN")
                .name("OTP Login")
                .body("Your Mana OTP is {{otp}}. Valid for {{mins}} mins.")
                .language(SmsLanguage.EN)
                .messageType(MessageType.OTP)
                .status(TemplateStatus.ACTIVE)
                .dltTemplateId(VALID_TEMPLATE_ID)
                .unicode(false)
                .maxLength(160)
                .build();

        ValidationResult result = validator.validateForDispatch(
                tpl, VALID_ENTITY_ID, Map.of("otp", "482910", "mins", "5"), 30);

        assertThat(result.compliant()).isTrue();
        assertThat(result.violations()).isEmpty();
        assertThat(result.containsIndicScript()).isFalse();
        assertThat(result.smsSegmentCount()).isEqualTo(1);
    }

    @Test
    void blocksVariableCharLimitOverflowAndMissingDltRegistration() {
        SmsTemplate tpl = SmsTemplate.builder()
                .templateCode("MAINT_REMINDER")
                .name("Maintenance Reminder")
                .body("Dear {{name}}, dues of Rs {{amount}} for {{flat_address}} are pending.")
                .language(SmsLanguage.EN)
                .messageType(MessageType.TRANSACTIONAL)
                .status(TemplateStatus.DRAFT) // not approved/active
                .dltTemplateId("BAD-ID")
                .unicode(false)
                .build();

        String longAddress = "Tower-A, Floor-12, Flat-1204, Emerald Residency Extension"; // > 30 chars
        ValidationResult result = validator.validateForDispatch(
                tpl, "123", Map.of("name", "Ravi", "amount", "5050", "flat_address", longAddress), 30);

        assertThat(result.compliant()).isFalse();
        assertThat(result.violations()).anySatisfy(v -> assertThat(v).contains("Entity ID"));
        assertThat(result.violations()).anySatisfy(v -> assertThat(v).contains("Template ID"));
        assertThat(result.violations()).anySatisfy(v -> assertThat(v).contains("DRAFT"));
        assertThat(result.violations()).anySatisfy(v -> assertThat(v).contains("flat_address").contains("30-char limit"));
    }

    @Test
    void enforcesUnicodeFlagAndUcs2SegmentMathForRegionalScripts() {
        SmsTemplate gsmOnlyTelugu = SmsTemplate.builder()
                .templateCode("VISITOR_ARRIVED_TE")
                .name("Visitor Arrived Telugu")
                .body("మీ అతిథి {{visitor}} గేట్ వద్ద ఉన్నారు.")
                .language(SmsLanguage.TE)
                .messageType(MessageType.TRANSACTIONAL)
                .status(TemplateStatus.ACTIVE)
                .dltTemplateId(VALID_TEMPLATE_ID)
                .unicode(false) // Misconfigured GSM-7 for Telugu
                .build();

        ValidationResult badUnicode = validator.validateForDispatch(
                gsmOnlyTelugu, VALID_ENTITY_ID, Map.of("visitor", "రమేష్"), 30);
        assertThat(badUnicode.compliant()).isFalse();
        assertThat(badUnicode.containsIndicScript()).isTrue();
        assertThat(badUnicode.violations()).anySatisfy(v -> assertThat(v).contains("unicode=false"));

        // Enable unicode=true and verify Telugu/Hindi passes with UCS-2 segment calculation
        gsmOnlyTelugu.setUnicode(true);
        ValidationResult okUnicode = validator.validateForDispatch(
                gsmOnlyTelugu, VALID_ENTITY_ID, Map.of("visitor", "రమేష్"), 30);
        assertThat(okUnicode.compliant()).isTrue();
        assertThat(okUnicode.smsSegmentCount()).isGreaterThanOrEqualTo(1);
    }
}
