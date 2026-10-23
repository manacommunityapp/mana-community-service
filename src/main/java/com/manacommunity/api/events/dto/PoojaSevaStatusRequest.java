package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.enums.PoojaSevaStatus;

public class PoojaSevaStatusRequest {
    private PoojaSevaStatus status;

    public PoojaSevaStatus getStatus() { return status; }
    public void setStatus(PoojaSevaStatus status) { this.status = status; }
}
