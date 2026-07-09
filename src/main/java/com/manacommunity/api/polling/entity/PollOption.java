package com.manacommunity.api.polling.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_option")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Poll poll;
}
