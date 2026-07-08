package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ListingResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String priceUnit;
    private String category;
    private String status;
    private String transactionMode;
    private String visibility;
    private String location;
    private List<String> imageUrls;
    private SellerRef seller;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class SellerRef {
        private Long id;
        private String fullName;
        private boolean verified;
    }
}
