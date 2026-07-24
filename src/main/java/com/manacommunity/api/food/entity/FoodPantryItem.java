package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_pantry_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(length = 100)
    private String category;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 100)
    private String barcode;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_location", length = 20)
    private StorageLocation storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PantryItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum StorageLocation { FRIDGE, FREEZER, PANTRY, COUNTER }

    public enum PantryItemStatus { AVAILABLE, LOW, EXPIRED, CONSUMED }
}
