package com.manacommunity.api.freshkitchen.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;

@Entity
@Table(name = "daily_basket_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "basket")
@ToString(exclude = "basket")
public class DailyBasketItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "basket_id", nullable = false)
    private DailyBasket basket;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FoodCategory category = FoodCategory.BATTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 50)
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.DAILY_BASKET;

    @Column(nullable = false)
    @Builder.Default
    private Boolean mon = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean tue = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean wed = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean thu = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean fri = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sat = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sun = true;

    @Column(name = "days_of_week", nullable = false, length = 80)
    @Builder.Default
    private String daysOfWeek = "MON,TUE,WED,THU,FRI,SAT,SUN";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    public boolean isActiveOnDay(DayOfWeek dayOfWeek) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        return switch (dayOfWeek) {
            case MONDAY -> Boolean.TRUE.equals(mon);
            case TUESDAY -> Boolean.TRUE.equals(tue);
            case WEDNESDAY -> Boolean.TRUE.equals(wed);
            case THURSDAY -> Boolean.TRUE.equals(thu);
            case FRIDAY -> Boolean.TRUE.equals(fri);
            case SATURDAY -> Boolean.TRUE.equals(sat);
            case SUNDAY -> Boolean.TRUE.equals(sun);
        };
    }

    public void syncDaysOfWeekString() {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(mon)) sb.append("MON,");
        if (Boolean.TRUE.equals(tue)) sb.append("TUE,");
        if (Boolean.TRUE.equals(wed)) sb.append("WED,");
        if (Boolean.TRUE.equals(thu)) sb.append("THU,");
        if (Boolean.TRUE.equals(fri)) sb.append("FRI,");
        if (Boolean.TRUE.equals(sat)) sb.append("SAT,");
        if (Boolean.TRUE.equals(sun)) sb.append("SUN,");
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        this.daysOfWeek = sb.toString();
    }
}
