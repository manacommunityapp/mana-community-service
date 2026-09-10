package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketLostAndFound;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketLostAndFoundRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Post type is required")
    private MarketLostAndFound.PostType type;

    private String category;
    private String imageUrl;
    private String location;
    private LocalDate dateOccurred;
}
