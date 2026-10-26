package com.manacommunity.api.visitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateGroupInviteRequest {
    private String title;
    private String eventDate;
    private String startTime;
    private String endTime;
    private List<GuestDto> guests;

    @Data
    public static class GuestDto {
        private String name;
        private String phone;
        private String vehicleNumber;
    }
}
