package com.manacommunity.api.sports.model;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SportsEvent getEvent() {
        return event;
    }

    public void setEvent(SportsEvent event) {
        this.event = event;
    }

    public SportsEvent.MatchFormat getFormat() {
        return format;
    }

    public void setFormat(SportsEvent.MatchFormat format) {
        this.format = format;
    }
}
