package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal_log", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "meal_time")
    private LocalTime mealTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private MealSourceType sourceType;

    @Column(name = "original_input", length = 1000)
    private String originalInput;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "total_calories")
    private Integer totalCalories;

    @Column(name = "total_protein")
    private BigDecimal totalProtein;

    @Column(name = "total_carbs")
    private BigDecimal totalCarbs;

    @Column(name = "total_fat")
    private BigDecimal totalFat;

    @Column(name = "total_fiber")
    private BigDecimal totalFiber;

    @Column(name = "ai_confidence")
    private BigDecimal aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MealLogStatus status;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "mealLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealItem> items = new ArrayList<>();
}
