package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceSenderType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_service_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextualChatMessageEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "booking_id", length = 64)
    private String bookingId;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "resident_user_id", nullable = false, length = 64)
    private String residentUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private HomeServiceSenderType senderType;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
