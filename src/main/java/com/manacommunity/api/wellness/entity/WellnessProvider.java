package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wellness_provider", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessProvider extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private WellnessProviderType providerType;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @Column(name = "consultation_fee")
    private BigDecimal consultationFee;

    @Column(name = "available_online")
    private Boolean availableOnline;

    @Column(name = "available_in_person")
    private Boolean availableInPerson;

    @Column(name = "location")
    private String location;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "verified")
    private Boolean verified;

    @Column(name = "active")
    private Boolean active;
}
