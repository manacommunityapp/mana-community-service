package com.manacommunity.api.dto.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Mirrors the Expo Push API v2 response shape.
 * Used to detect invalid tokens (DeviceNotRegistered) and deactivate them.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpoPushResponse {

    private List<TicketData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TicketData {
        /** "ok" or "error" */
        private String status;
        /** Present when status = "ok" */
        private String id;
        /** Present when status = "error" */
        private String message;
        private Details details;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {
        /**
         * "DeviceNotRegistered" — the token is no longer valid.
         * We deactivate it immediately to avoid wasting future sends.
         */
        private String error;
    }
}
