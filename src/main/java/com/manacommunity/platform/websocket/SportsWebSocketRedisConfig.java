package com.manacommunity.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
public class SportsWebSocketRedisConfig {

    // ── Channel name constants ────────────────────────────────────────────────
    public static final String MATCH_EVENTS_CHANNEL    = "sports:match:events";
    public static final String AUCTION_EVENTS_CHANNEL  = "sports:auction:events";
    public static final String FEED_EVENTS_CHANNEL     = "sports:feed:events";
    public static final String EVENT_AUCTION_CHANNEL   = "community:event-auction:events";
    public static final String GROUP_ORDER_CHANNEL     = "community:grouporder:events";
    public static final String IOT_ALERTS_CHANNEL      = "community:iot:alerts";

    // ── Topic beans ───────────────────────────────────────────────────────────

    @Bean public ChannelTopic sportsMatchEventsTopic()   { return new ChannelTopic(MATCH_EVENTS_CHANNEL); }
    @Bean public ChannelTopic sportsAuctionEventsTopic() { return new ChannelTopic(AUCTION_EVENTS_CHANNEL); }
    @Bean public ChannelTopic sportsFeedEventsTopic()    { return new ChannelTopic(FEED_EVENTS_CHANNEL); }
    @Bean public ChannelTopic eventAuctionTopic()        { return new ChannelTopic(EVENT_AUCTION_CHANNEL); }
    @Bean public ChannelTopic groupOrderTopic()          { return new ChannelTopic(GROUP_ORDER_CHANNEL); }
    @Bean public ChannelTopic iotAlertsTopic()           { return new ChannelTopic(IOT_ALERTS_CHANNEL); }

    // ── Single listener container for all channels ───────────────────────────

    @Bean
    public RedisMessageListenerContainer sportsRedisMessageListenerContainer(
            @Autowired(required = false) RedisConnectionFactory connectionFactory,
            SportsMatchRedisSubscriber    matchSubscriber,
            SportsAuctionRedisSubscriber  auctionSubscriber,
            SportsFeedRedisSubscriber     feedSubscriber,
            EventAuctionRedisSubscriber   eventAuctionSubscriber,
            GroupOrderRedisSubscriber     groupOrderSubscriber,
            IoTAlertsRedisSubscriber      iotAlertsSubscriber,
            ChannelTopic sportsMatchEventsTopic,
            ChannelTopic sportsAuctionEventsTopic,
            ChannelTopic sportsFeedEventsTopic,
            ChannelTopic eventAuctionTopic,
            ChannelTopic groupOrderTopic,
            ChannelTopic iotAlertsTopic) {

        if (connectionFactory == null) {
            log.warn("RedisConnectionFactory not available — all Redis Pub/Sub channels disabled (fallback to local in-memory messaging).");
            return null;
        }

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(matchSubscriber,       sportsMatchEventsTopic);
        container.addMessageListener(auctionSubscriber,     sportsAuctionEventsTopic);
        container.addMessageListener(feedSubscriber,        sportsFeedEventsTopic);
        container.addMessageListener(eventAuctionSubscriber,eventAuctionTopic);
        container.addMessageListener(groupOrderSubscriber,  groupOrderTopic);
        container.addMessageListener(iotAlertsSubscriber,   iotAlertsTopic);

        log.info("Initialized RedisMessageListenerContainer for channels: {}, {}, {}, {}, {}, {}",
                MATCH_EVENTS_CHANNEL, AUCTION_EVENTS_CHANNEL, FEED_EVENTS_CHANNEL,
                EVENT_AUCTION_CHANNEL, GROUP_ORDER_CHANNEL, IOT_ALERTS_CHANNEL);
        return container;
    }
}
