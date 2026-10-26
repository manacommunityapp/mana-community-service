package com.manacommunity.api.watchlist.dto;

import lombok.Data;

@Data
public class WatchlistRequest {
    private String name;
    private String phone;
    private String vehicleNumber;
    private String photo;
    private String reason;
    private String category;
    private String notes;
}
