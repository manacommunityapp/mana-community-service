package com.manacommunity.api.notification.router;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class NotificationRouter {

    private final List<NotificationChannel> channels = new CopyOnWriteArrayList<>();
    private final Set<Long> dndUsers = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final long FALLBACK_TIMEOUT_MS = 10_000;

    public void registerChannel(NotificationChannel channel) {
        channels.add(channel);
        channels.sort(Comparator.comparingInt(NotificationChannel::priorityOrder));
        log.info("Registered notification channel: {} (priority {})", channel.channelId(), channel.priorityOrder());
    }

    public void setDnd(Long userId, boolean enabled) {
        if (enabled) dndUsers.add(userId); else dndUsers.remove(userId);
    }

    public RoutingResult route(NotificationRequest request) {
        List<NotificationChannel> eligible = channels.stream()
                .filter(ch -> ch.supports(request))
                .collect(Collectors.toList());

        if (eligible.isEmpty()) {
            return new RoutingResult(false, List.of(), "No eligible channels for this notification");
        }

        boolean isDnd = dndUsers.contains(request.recipientId());
        if (isDnd && !request.dndOverride() && !request.isUrgent()) {
            log.info("DND active for user {}, notification suppressed: {}", request.recipientId(), request.title());
            return new RoutingResult(false, List.of(), "Recipient has DND enabled; notification is not urgent enough to override");
        }

        if (isDnd && (request.dndOverride() || request.isUrgent())) {
            log.info("DND override for user {} — urgent notification: {}", request.recipientId(), request.title());
        }

        List<DeliveryResult> attempts = new ArrayList<>();

        for (NotificationChannel channel : eligible) {
            Future<DeliveryResult> future = executor.submit(() -> channel.send(request));

            try {
                DeliveryResult result = future.get(FALLBACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                attempts.add(result);

                if (result.success()) {
                    log.info("Notification delivered via {} in {}ms: {}", channel.channelId(),
                            result.deliveryTimeMs(), request.title());
                    return new RoutingResult(true, attempts, null);
                }

                log.warn("Channel {} failed for '{}': {}, falling back", channel.channelId(),
                        request.title(), result.errorReason());

            } catch (TimeoutException e) {
                future.cancel(true);
                DeliveryResult timeout = DeliveryResult.failure(channel.channelId(),
                        "Delivery timeout (" + FALLBACK_TIMEOUT_MS + "ms)", FALLBACK_TIMEOUT_MS);
                attempts.add(timeout);
                log.warn("Channel {} timed out for '{}', falling back to next", channel.channelId(), request.title());

            } catch (Exception e) {
                DeliveryResult error = DeliveryResult.failure(channel.channelId(), e.getMessage(), 0);
                attempts.add(error);
            }
        }

        log.error("All channels exhausted for notification '{}' to user {}", request.title(), request.recipientId());
        return new RoutingResult(false, attempts, "All channels failed");
    }

    public List<ChannelInfo> listChannels() {
        return channels.stream()
                .map(ch -> new ChannelInfo(ch.channelId(), ch.priorityOrder()))
                .toList();
    }

    public record RoutingResult(boolean delivered, List<DeliveryResult> attempts, String failureReason) {}
    public record ChannelInfo(String channelId, int priority) {}
}
