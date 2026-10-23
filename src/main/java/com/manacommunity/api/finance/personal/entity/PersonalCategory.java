package com.manacommunity.api.finance.personal.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_finance_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalCategory {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user; // Null if system default

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String icon = "ellipse-outline";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String color = "#6B7280";

    @Column(nullable = false, length = 20)
    private String type; // INCOME, EXPENSE

    @Column(name = "parent_id", length = 64)
    private String parentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
