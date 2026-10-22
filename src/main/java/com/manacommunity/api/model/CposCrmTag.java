package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpos_crm_tag")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CposCrmTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String color = "#4F6AF5";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
