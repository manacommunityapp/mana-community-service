package com.manacommunity.api.freshkitchen.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "daily_basket",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_basket_user_community", columnNames = {"user_id", "community_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "items")
@ToString(exclude = "items")
public class DailyBasket extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private Boolean isPaused = false;

    @Column(name = "pause_start_date")
    private LocalDate pauseStartDate;

    @Column(name = "pause_end_date")
    private LocalDate pauseEndDate;

    @Column(name = "delivery_slot", nullable = false, length = 60)
    @Builder.Default
    private String deliverySlot = "MORNING_06_00_07_00";

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "basket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyBasketItem> items = new ArrayList<>();

    public boolean isPausedOn(LocalDate date) {
        if (!Boolean.TRUE.equals(isPaused)) {
            return false;
        }
        if (pauseStartDate == null) {
            return true;
        }
        if (date.isBefore(pauseStartDate)) {
            return false;
        }
        return pauseEndDate == null || !date.isAfter(pauseEndDate);
    }
}
