package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_basket")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBasket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long communityId;
    private Long userId;
    private Long vendorId;
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "basket_frequency")
    private BasketFrequency frequency;
    private LocalDate nextOrderDate;
    private Long slotTemplateId;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "basket_status")
    private BasketStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public enum BasketFrequency { DAILY, WEEKLY, FORTNIGHTLY, MONTHLY }
    public enum BasketStatus { ACTIVE, PAUSED, CANCELLED }
}
