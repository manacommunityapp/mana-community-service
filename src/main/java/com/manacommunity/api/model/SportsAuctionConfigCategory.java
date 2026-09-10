package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table(name = "sports_auction_config_category")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class SportsAuctionConfigCategory {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private SportsAuctionConfig config;

    @Column(nullable = false)
    private String categoryName;

    public SportsAuctionConfigCategory(SportsAuctionConfig config, String categoryName) {
        this.config = config;
        this.categoryName = categoryName;
    }
}
