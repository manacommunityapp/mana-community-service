package com.manacommunity.api.notification.orchestrator.engine;

import com.manacommunity.api.notification.orchestrator.event.DomainEvent;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    private final AppUserRepository userRepository;

    public List<Long> resolveRecipients(DomainEvent event, NotificationRule rule) {
        String targetType = event.getTargetType() != null ? event.getTargetType() : rule.getDefaultTargetType();
        String targetId = event.getTargetId();

        if (event.getDirectRecipientUserId() != null) {
            return List.of(event.getDirectRecipientUserId());
        }

        switch (targetType.toUpperCase()) {
            case "DIRECT_USER" -> {
                if (targetId != null) {
                    try {
                        return List.of(Long.parseLong(targetId));
                    } catch (NumberFormatException ignored) {}
                }
                return Collections.emptyList();
            }
            case "TOWER" -> {
                if (targetId != null) {
                    return userRepository.findAll().stream()
                            .filter(u -> targetId.equalsIgnoreCase(u.getTower()))
                            .map(AppUser::getId)
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
            case "ROLE" -> {
                if (targetId != null) {
                    return userRepository.findAll().stream()
                            .filter(u -> u.getRole() != null && targetId.equalsIgnoreCase(u.getRole().toString()))
                            .map(AppUser::getId)
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
            case "COMMUNITY" -> {
                return userRepository.findAll().stream()
                        .map(AppUser::getId)
                        .limit(200) // Safety boundary
                        .collect(Collectors.toList());
            }
            default -> {
                log.warn("Unknown targetType: {}", targetType);
                return Collections.emptyList();
            }
        }
    }
}
