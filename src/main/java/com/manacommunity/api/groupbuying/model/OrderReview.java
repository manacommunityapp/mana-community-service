package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_buy_order_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReview extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "deal_id", nullable = false, length = 50)
    private String dealId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "resident_name", length = 100)
    private String residentName;

    @Column(name = "product_rating", nullable = false)
    private Integer productRating;

    @Column(name = "delivery_rating", nullable = false)
    private Integer deliveryRating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
