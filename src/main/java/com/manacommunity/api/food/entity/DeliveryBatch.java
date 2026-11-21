package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_batch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long communityId;
    private Long vendorId;
    private Long slotId;
    private String towerBlock;
    private int batchSequence;
    private int orderCount;
    private Long assignedTo;
    private Instant createdAt;
}
