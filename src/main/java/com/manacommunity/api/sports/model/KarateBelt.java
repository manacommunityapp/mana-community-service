package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "karate_belt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarateBelt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;           // White, Yellow, Orange, Green, Blue, Purple, Brown, Red, Black

    @Column(nullable = false)
    private Integer sortOrder;     // 1–9 for ordering in UI

    @Column(nullable = false, length = 30)
    private String colorHex;       // e.g. "#FFFFFF"

    @Column(length = 200)
    private String description;
}
