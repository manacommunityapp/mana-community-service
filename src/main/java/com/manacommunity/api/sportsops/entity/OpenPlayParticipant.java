package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "open_play_participant")
@Data
public class OpenPlayParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "joined_at") private Instant joinedAt = Instant.now();
    @Column private Boolean confirmed = false;
}
