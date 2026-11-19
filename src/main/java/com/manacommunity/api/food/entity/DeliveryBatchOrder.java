package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_batch_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryBatchOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long batchId;
    private Long orderId;
    private String orderType;
    private String flatNumber;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "cold_chain_type")
    private ColdChainType coldChainType;
    private Instant deliveredAt;
    private String deliveryNotes;

    public enum ColdChainType { AMBIENT, CHILLED, FROZEN }
}
