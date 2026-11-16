package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_match_stream_message",
        indexes = @Index(name = "idx_stream_msg_stream", columnList = "stream_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsMatchStreamMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", nullable = false)
    private SportsMatchStream stream;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, length = 500)
    private String text;

    /** True for official commentary posted by the organiser */
    @Column(name = "is_commentary", nullable = false)
    @Builder.Default
    private Boolean commentary = false;

    @Column(name = "posted_at", nullable = false)
    @Builder.Default
    private LocalDateTime postedAt = LocalDateTime.now();
}
