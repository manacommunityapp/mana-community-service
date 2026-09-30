package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Acknowledgment pushed to the bidder via /user/queue/auction/bid-result
 * after the server processes a bid submitted over STOMP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidAckMessage {

    public enum BidAckStatus {
        ACCEPTED, REJECTED, OUTBID
    }

    /** Final outcome of the bid attempt. */
    private BidAckStatus status;

    /** Saved bid ID (present only on ACCEPTED). */
    private Long bidId;

    /** The auction item this ack relates to. */
    private Long auctionId;

    /** The amount that was bid. */
    private BigDecimal amount;

    /** Human-readable reason for REJECTED or OUTBID. */
    private String reason;
}
