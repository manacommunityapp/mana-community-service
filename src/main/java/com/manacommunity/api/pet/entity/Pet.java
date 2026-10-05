package com.manacommunity.api.pet.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pet", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Pet extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "species", nullable = false, length = 50)
    @Builder.Default
    private String species = "DOG";

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "color", length = 80)
    private String color;

    @Column(name = "age_months")
    private Integer ageMonths;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private java.math.BigDecimal weightKg;

    @Column(name = "microchip_id", length = 50)
    private String microchipId;

    @Column(name = "is_vaccinated", nullable = false)
    @Builder.Default
    private boolean vaccinated = false;

    @Column(name = "vaccine_expiry_date")
    private LocalDate vaccineExpiryDate;

    @Column(name = "registration_date", nullable = false)
    @Builder.Default
    private LocalDate registrationDate = LocalDate.now();

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
