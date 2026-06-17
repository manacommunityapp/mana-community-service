package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Join row linking an {@link AppUser} to a {@link Conversation}.
 *
 * {@code lastReadAt} is the watermark used to compute the per-user unread
 * count (messages created after it, not sent by the user, are unread).
 */
@Entity
@Table(name = "chat_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_participant_conv_user",
                columnNames = {"conversation_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
