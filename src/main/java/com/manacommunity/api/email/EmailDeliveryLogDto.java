package com.manacommunity.api.email;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO returned by the admin email delivery log endpoint.
 * Uses a Java record so serialization is zero-boilerplate.
 */
@Builder
public record EmailDeliveryLogDto(
        Long id,
        String sender,
        String recipient,
        String subject,
        String body,
        String templateType,
        String status,
        String errorMessage,
        boolean opened,
        LocalDateTime openedAt,
        Long communityId,
        LocalDateTime sentAt
) {
    /** Maps an {@link EmailDeliveryLog} entity to this DTO. */
    public static EmailDeliveryLogDto from(EmailDeliveryLog log) {
        return EmailDeliveryLogDto.builder()
                .id(log.getId())
                .sender(log.getSender())
                .recipient(log.getRecipient())
                .subject(log.getSubject())
                .body(log.getBody())
                .templateType(log.getTemplateType())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .opened(log.getOpenedAt() != null)
                .openedAt(log.getOpenedAt())
                .communityId(log.getCommunityId())
                .sentAt(log.getSentAt())
                .build();
    }
}
