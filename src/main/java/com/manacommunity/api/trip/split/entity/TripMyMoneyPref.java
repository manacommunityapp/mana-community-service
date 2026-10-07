package com.manacommunity.api.trip.split.entity;

import jakarta.persistence.*;
import lombok.*;

/** A user's My Money integration choice for one trip. No row means OFF. */
@Entity
@Table(name = "trip_my_money_pref", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMyMoneyPref {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, length = 50)
    private String tripId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MyMoneyMode mode = MyMoneyMode.OFF;
}
