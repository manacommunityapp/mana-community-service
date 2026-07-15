package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_sequence")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventorySequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String prefix;

    @Column(name = "next_val", nullable = false)
    @Builder.Default
    private Long nextVal = 1L;
}
