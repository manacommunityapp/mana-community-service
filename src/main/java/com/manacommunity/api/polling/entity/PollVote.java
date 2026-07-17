package com.manacommunity.api.polling.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Distinct JPA entity name + table: the legacy feed entity
// com.manacommunity.api.model.PollVote also uses the entity name "PollVote"
// and the table "poll_vote" (with post-based columns). Two entities cannot
// share an entity name (Hibernate bootstrap fails) or a table with different
// columns, so this standalone polling module's votes live in their own table.
@Entity(name = "PollingPollVote")
@Table(name = "poll_option_vote", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"poll_id", "voter_id", "option_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private AppUser voter;

    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;

    @PrePersist
    protected void onCreate() {
        votedAt = LocalDateTime.now();
    }
}
