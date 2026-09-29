# Push Notification — Integration Guide

## What was delivered

| File | Purpose |
|---|---|
| `model/PushToken.java`              | Entity: one row per device |
| `repository/PushTokenRepository.java` | Queries for active tokens by user / community |
| `dto/push/RegisterPushTokenRequest.java` | Mobile sends this on login |
| `dto/push/ExpoPushMessage.java`     | Expo API request payload |
| `dto/push/ExpoPushResponse.java`    | Expo API response — for ticket processing |
| `event/ChatMessageSentEvent.java`   | Spring event: chat message sent |
| `event/AuctionBidPlacedEvent.java`  | Spring event: bid accepted |
| `event/AuctionEndedEvent.java`      | Spring event: auction closed |
| `event/CommunityEventCreatedEvent.java` | Spring event: new fixture/event |
| `event/AnnouncementCreatedEvent.java` | Spring event: admin announcement |
| `event/NewPostEvent.java`           | Spring event: new feed post/poll |
| `service/ExpoPushService.java`      | Interface with typed helper methods |
| `service/impl/ExpoPushServiceImpl.java` | Expo API v2, batching, token cleanup |
| `scheduler/PushNotificationServiceImpl.java` | Replaces the stub — drop-in |
| `listener/MobilePushEventListener.java` | @Async event → push dispatch |
| `controller/PushTokenController.java` | POST/DELETE /api/users/push-token |
| `config/RestClientConfig.java`      | RestTemplate + async thread pool |
| `scheduler/PushTokenCleanupScheduler.java` | Weekly stale-token purge |
| `db/migration/V5__push_tokens.sql`  | PostgreSQL migration |
| `resources/application-push.yaml`  | Config properties |

---

## Step 1 — Run the SQL migration

```bash
psql -U postgres -d manacommunity -f V5__push_tokens.sql
```

Or if using Flyway, copy `V5__push_tokens.sql` to `src/main/resources/db/migration/`.

---

## Step 2 — Remove the stub

In `PushNotificationServiceStub.java`, either:
- Delete the file entirely, OR
- Remove `@Service` (leave the class for reference)

`PushNotificationServiceImpl` is `@Primary` so Spring prefers it, but two `@Service`
beans for the same interface will cause a "expected single matching bean" error.

---

## Step 3 — Wire 3 lines into existing services

Each block below is a **minimal, surgical change** — inject `ApplicationEventPublisher`
and call `events.publishEvent(...)` after the existing save/persist call.

### 3a. ChatServiceImpl — notify on new message

```java
// Add to ChatServiceImpl:
@Autowired
private ApplicationEventPublisher events;

// Inside sendMessage(...), after saving the ChatMessage:
List<Long> recipientIds = conversation.getParticipants().stream()
    .map(p -> p.getUser().getId())
    .filter(id -> !id.equals(sender.getId()))
    .collect(Collectors.toList());

events.publishEvent(new ChatMessageSentEvent(
    this,
    savedMessage.getId(),
    conversation.getId(),
    sender.getId(),
    sender.getFullName(),
    savedMessage.getContent(),
    recipientIds
));
```

### 3b. AuctionServiceImpl — notify on bid

```java
// Add to AuctionServiceImpl:
@Autowired
private ApplicationEventPublisher events;

// After bidRepo.save(bid) succeeds:
events.publishEvent(new AuctionBidPlacedEvent(
    this,
    config.getId(),             // auctionId
    config.getTitle(),          // or player name for player auctions
    biddingUserId,              // new bidder
    bidByUser.getFullName(),
    req.bidAmount(),
    previousLeaderUserId,       // null if first bid; query bidRepo for previous max bid
    config.getCommunity().getId()
));
```

### 3c. AdminServiceImpl — notify on announcement

```java
// In createAnnouncement(), after announcementRepo.save(ann):
events.publishEvent(new AnnouncementCreatedEvent(
    this,
    ann.getId(),
    ann.getTitle(),
    ann.getContent(),
    ann.getPriority(),
    community.getId(),
    admin.getId()
));
```

### 3d. EventService / EventController — notify on new event

```java
// After saving the event:
events.publishEvent(new CommunityEventCreatedEvent(
    this,
    savedEvent.getId(),
    savedEvent.getTitle(),
    savedEvent.getVenue(),
    savedEvent.getStartAt().toString(),
    community.getId()
));
```

---

## Step 4 — Verify `@EnableScheduling`

`PushTokenCleanupScheduler` uses `@Scheduled`. Ensure your main application class
or any `@Configuration` class has `@EnableScheduling`.

```java
@SpringBootApplication
@EnableScheduling   // ← add this if not already present
public class ManaCommunityApplication { ... }
```

---

## Step 5 — Test end-to-end

```bash
# 1. Start the app and log in via the mobile app to register a token
# 2. Send a chat message — watch notification.log
tail -f logs/notification.log

# 3. Manually trigger a push (useful for testing without the mobile app):
curl -X POST http://localhost:8082/api/admin/announcements \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Push","content":"Hello world","priority":"NORMAL"}'

# If the mobile app is in the background, you should see the notification
# appear within ~200ms.
```

---

## How push notifications route in the mobile app

The mobile hook `usePushNotifications.ts` reads `notification.request.content.data.type`
when a notification is tapped and navigates accordingly:

| `type` value         | Navigates to               |
|----------------------|----------------------------|
| `NEW_MESSAGE`        | `/chat/{conversationId}`   |
| `AUCTION_BID`        | `/auction/{auctionId}`     |
| `AUCTION_ENDED`      | `/auction/{auctionId}`     |
| `AUCTION_SOLD`       | `/auction/{auctionId}`     |
| `NEW_EVENT`          | `/tabs/events`             |
| `ANNOUNCEMENT`       | `/tabs/feed`               |
| `NEW_POST`           | `/tabs/feed`               |
| `GENERAL`            | `/tabs/feed`               |

---

## Expo Push API limits

| Limit | Value |
|---|---|
| Messages per request | 100 (batched automatically) |
| Rate limit | 600 req/min per project |
| Max payload size | 4 KB per message |
| Token format | `ExponentPushToken[...]` |
| Free tier | Unlimited (no quota on Expo's free plan) |

Expo receipts (async delivery confirmation) can be fetched via
`POST https://exp.host/--/api/v2/push/getReceipts` using the ticket `id`
returned in `ExpoPushResponse.TicketData`. The current implementation processes
tickets synchronously — add a scheduled receipt-check job if you need
guaranteed delivery tracking.
