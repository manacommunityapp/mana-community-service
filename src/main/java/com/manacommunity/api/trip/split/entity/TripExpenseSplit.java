package com.manacommunity.api.trip.split.entity;

import jakarta.persistence.*;
import lombok.*;

/** The computed share one user owes for one expense. Shares of an expense sum to its total. */
@Entity
@Table(name = "trip_expense_split", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "share_paise", nullable = false)
    private long sharePaise;
}
