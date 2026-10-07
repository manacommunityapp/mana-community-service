package com.manacommunity.api.trip.split.entity;

import jakarta.persistence.*;
import lombok.*;

/** The inputs a user chose for one participant of one expense. */
@Entity
@Table(name = "trip_expense_participant", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripExpenseParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private long weight = 1;

    @Column(nullable = false)
    @Builder.Default
    private long quantity = 1;

    @Column(name = "percentage_bp", nullable = false)
    @Builder.Default
    private long percentageBp = 0;

    @Column(name = "exact_paise", nullable = false)
    @Builder.Default
    private long exactPaise = 0;
}
