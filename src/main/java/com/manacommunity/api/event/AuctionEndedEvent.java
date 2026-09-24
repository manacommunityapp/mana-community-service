package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when an auction item is sold or the auction closes with no bids.
 * Notifies the winner, the seller, and all participants who placed bids.
 */
@Getter
public class AuctionEndedEvent extends ApplicationEvent {

    private final Long         auctionId;
    private final String       auctionTitle;
    private final Long         winnerId;       // null if no bids
    private final String       winnerName;
    private final long         finalAmount;
    private final Long         sellerId;
    /** All users who placed at least one bid (excluding the winner). */
    private final java.util.List<Long> otherBidderIds;
    private final Long         communityId;

    public AuctionEndedEvent(Object source,
                             Long auctionId,
                             String auctionTitle,
                             Long winnerId,
                             String winnerName,
                             long finalAmount,
                             Long sellerId,
                             java.util.List<Long> otherBidderIds,
                             Long communityId) {
        super(source);
        this.auctionId      = auctionId;
        this.auctionTitle   = auctionTitle;
        this.winnerId       = winnerId;
        this.winnerName     = winnerName;
        this.finalAmount    = finalAmount;
        this.sellerId       = sellerId;
        this.otherBidderIds = otherBidderIds != null ? otherBidderIds : java.util.List.of();
        this.communityId    = communityId;
    }
}
