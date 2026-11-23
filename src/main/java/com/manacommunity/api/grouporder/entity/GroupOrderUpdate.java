package com.manacommunity.api.grouporder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_order_updates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupOrderUpdate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_order_id", nullable = false)
    private Long groupOrderId;

    @Column(name = "update_type", nullable = false, length = 50)
    private String updateType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "posted_by", nullable = false)
    private Long postedBy;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private OffsetDateTime postedAt = OffsetDateTime.now();
}
