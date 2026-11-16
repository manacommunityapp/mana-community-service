package com.manacommunity.api.socialcircle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_game_number_call", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyGameNumberCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private KittyPartyGameRoom room;

    @Column(name = "sequence_num", nullable = false)
    private Integer sequenceNum;

    @Column(name = "called_number", nullable = false)
    private Integer calledNumber;

    @Column(name = "called_at", nullable = false, updatable = false)
    private LocalDateTime calledAt;

    @PrePersist
    protected void onCreate() {
        calledAt = LocalDateTime.now();
    }
}
