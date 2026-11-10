package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "home_service_category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "icon_url", length = 300)
    private String iconUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
