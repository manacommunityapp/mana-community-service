package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published by AuctionServiceImpl after a bid is accepted and saved.
 *
 * Usage in AuctionServiceImpl (after saving the bid):
 * <pre>
 *   events.publishEvent(new AuctionBidPlacedEvent(this,
 *       auctionId,
 *       auctionTitle,
 *       newBidderId,
 *       newBidderName,
 *       bidAmount,
 *       previousLeaderId,   // null if first bid
 *       communityId));
 * </pre>
 */
@Getter
public class AuctionBidPlacedEvent extends ApplicationEvent {

    private final Long   auctionId;
    private final String auctionTitle;
    private final Long   newBidderId;
    private final String newBidderName;
    private final long   bidAmount;
    /** Null if this is the first bid on the item. */
    private final Long   previousLeaderId;
    private final Long   communityId;

    public AuctionBidPlacedEvent(Object source,
                                 Long auctionId,
                                 String auctionTitle,
                                 Long newBidderId,
                                 String newBidderName,
                                 long bidAmount,
                                 Long previousLeaderId,
                                 Long communityId) {
        super(source);
        this.auctionId        = auctionId;
        this.auctionTitle     = auctionTitle;
        this.newBidderId      = newBidderId;
        this.newBidderName    = newBidderName;
        this.bidAmount        = bidAmount;
        this.previousLeaderId = previousLeaderId;
        this.communityId      = communityId;
    }
}
