package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_event_format")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "event")
@EqualsAndHashCode(exclude = "event")
public class SportsEventFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private SportsEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 30)
    private SportsEvent.MatchFormat format;
}
