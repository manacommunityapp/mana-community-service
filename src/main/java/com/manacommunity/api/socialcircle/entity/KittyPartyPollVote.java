package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "kitty_poll_vote",
    schema = "manacommunity",
    uniqueConstraints = @UniqueConstraint(name = "uq_kitty_poll_user_post", columnNames = {"post_id", "user_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyPollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private KittyPartyPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private KittyPartyPollOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "voted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime votedAt = LocalDateTime.now();
}
