package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "commerce_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceReview extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommerceChannel channel;

    @Column(name = "target_type", length = 32, nullable = false)
    private String targetType;

    @Column(name = "target_id", length = 64, nullable = false)
    private String targetId;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "on_time_score")
    private Integer onTimeScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "photos_json", columnDefinition = "TEXT")
    private String photosJson;
}