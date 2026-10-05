package com.manacommunity.api.notification.orchestrator.engine;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.event.DomainEvent;
import com.manacommunity.api.notification.orchestrator.event.DomainEventType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationRuleEngine {

    private final Map<DomainEventType, NotificationRule> rules = new ConcurrentHashMap<>();

    public NotificationRuleEngine() {
        initDefaultRules();
    }

    private void initDefaultRules() {
        registerRule(NotificationRule.builder()
                .ruleId("RULE-EMERGENCY")
                .eventType(DomainEventType.EMERGENCY_BROADCAST)
                .category(NotificationCategory.EMERGENCY_SOS)
                .defaultPriority(NotificationPriority.CRITICAL)
                .defaultStrategy(DeliveryStrategy.ALL_CHANNELS)
                .defaultChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH, NotificationChannel.SMS, NotificationChannel.WHATSAPP))
                .titleTemplate("🚨 EMERGENCY: ${title}")
                .bodyTemplate("${body}")
                .defaultTargetType("COMMUNITY")
                .actionUrlTemplate("/emergency")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-SECURITY")
                .eventType(DomainEventType.SECURITY_ALERT)
                .category(NotificationCategory.EMERGENCY_SOS)
                .defaultPriority(NotificationPriority.CRITICAL)
                .defaultStrategy(DeliveryStrategy.ALL_CHANNELS)
                .defaultChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH, NotificationChannel.WHATSAPP))
                .titleTemplate("🛡️ Security Alert: ${gateName}")
                .bodyTemplate("${details}")
                .defaultTargetType("DIRECT_USER")
                .actionUrlTemplate("/gate")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-VISITOR")
                .eventType(DomainEventType.VISITOR_ARRIVED)
                .category(NotificationCategory.GATE_ACCESS)
                .defaultPriority(NotificationPriority.HIGH)
                .defaultStrategy(DeliveryStrategy.FALLBACK_CASCADE)
                .defaultChannels(List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP, NotificationChannel.SMS))
                .titleTemplate("Visitor at Gate: ${visitorName}")
                .bodyTemplate("${visitorName} (${vehicleNumber}) has arrived at ${gateName}. Pass: ${passCode}")
                .defaultTargetType("DIRECT_USER")
                .actionUrlTemplate("/gate/passes")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-BILL-GEN")
                .eventType(DomainEventType.BILL_GENERATED)
                .category(NotificationCategory.FINANCIAL_BILLING)
                .defaultPriority(NotificationPriority.HIGH)
                .defaultStrategy(DeliveryStrategy.PREFERENCE_BASED)
                .defaultChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH, NotificationChannel.EMAIL))
                .titleTemplate("New Maintenance Invoice: ₹${amount}")
                .bodyTemplate("Invoice #${invoiceNumber} for ${monthYear} is generated. Due by ${dueDate}.")
                .defaultTargetType("DIRECT_USER")
                .actionUrlTemplate("/billing")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-PAYMENT-OVERDUE")
                .eventType(DomainEventType.PAYMENT_OVERDUE)
                .category(NotificationCategory.FINANCIAL_BILLING)
                .defaultPriority(NotificationPriority.HIGH)
                .defaultStrategy(DeliveryStrategy.FALLBACK_CASCADE)
                .defaultChannels(List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP, NotificationChannel.SMS))
                .titleTemplate("⚠️ Payment Overdue: ₹${amount}")
                .bodyTemplate("Maintenance dues for ${monthYear} are past due. Please pay to avoid penalties.")
                .defaultTargetType("DIRECT_USER")
                .actionUrlTemplate("/billing")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-GROUP-BUY")
                .eventType(DomainEventType.GROUP_BUY_UNLOCKED)
                .category(NotificationCategory.COMMUNITY_NOTICE)
                .defaultPriority(NotificationPriority.NORMAL)
                .defaultStrategy(DeliveryStrategy.PREFERENCE_BASED)
                .defaultChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
                .titleTemplate("🔥 Deal Unlocked: ${productName}")
                .bodyTemplate("Tier ${tier} price of ₹${price} unlocked! Delivery scheduled for ${deliveryDate}.")
                .defaultTargetType("COMMUNITY")
                .actionUrlTemplate("/deals/${dealId}")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-PARKING-VIOLATION")
                .eventType(DomainEventType.PARKING_VIOLATION)
                .category(NotificationCategory.HELPDESK_TICKET)
                .defaultPriority(NotificationPriority.HIGH)
                .defaultStrategy(DeliveryStrategy.FALLBACK_CASCADE)
                .defaultChannels(List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP))
                .titleTemplate("Parking Notice: Slot ${spotNumber}")
                .bodyTemplate("Vehicle ${vehicleNumber} reported for ${violationType} at ${level}.")
                .defaultTargetType("DIRECT_USER")
                .actionUrlTemplate("/parking")
                .build());

        registerRule(NotificationRule.builder()
                .ruleId("RULE-ANNOUNCEMENT")
                .eventType(DomainEventType.COMMUNITY_ANNOUNCEMENT)
                .category(NotificationCategory.COMMUNITY_NOTICE)
                .defaultPriority(NotificationPriority.NORMAL)
                .defaultStrategy(DeliveryStrategy.PREFERENCE_BASED)
                .defaultChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH))
                .titleTemplate("${title}")
                .bodyTemplate("${body}")
                .defaultTargetType("COMMUNITY")
                .actionUrlTemplate("/feed")
                .build());
    }

    public void registerRule(NotificationRule rule) {
        rules.put(rule.getEventType(), rule);
    }

    public Optional<NotificationRule> findRule(DomainEventType eventType) {
        return Optional.ofNullable(rules.get(eventType));
    }

    public List<NotificationRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }

    public String renderTemplate(String template, Map<String, Object> payload) {
        if (template == null || payload == null) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }
}
