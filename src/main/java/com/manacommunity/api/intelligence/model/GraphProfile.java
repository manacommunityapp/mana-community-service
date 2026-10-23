package com.manacommunity.api.intelligence.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community_graph_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "tower", length = 30)
    private String tower;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "professions_json", columnDefinition = "TEXT")
    @Builder.Default
    private String professionsJson = "[]";

    @Column(name = "skills_json", columnDefinition = "TEXT")
    @Builder.Default
    private String skillsJson = "[]";

    @Column(name = "interests_json", columnDefinition = "TEXT")
    @Builder.Default
    private String interestsJson = "[]";

    @Column(name = "sports_json", columnDefinition = "TEXT")
    @Builder.Default
    private String sportsJson = "[]";

    @Column(name = "availability_hours", length = 100)
    private String availabilityHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}