package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.dto.EventAuctionBidRequest;
import com.manacommunity.api.events.dto.EventAuctionBidResponse;
import com.manacommunity.api.events.dto.EventAuctionItemRequest;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import com.manacommunity.api.events.dto.EventAuctionStatsResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventAuctionBid;
import com.manacommunity.api.events.entity.EventAuctionItem;
import com.manacommunity.api.events.entity.EventAuctionItem.ItemStatus;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventAuctionBidRepository;
import com.manacommunity.api.events.repository.EventAuctionItemRepository;
import com.manacommunity.api.events.service.EventAuctionService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAuctionServiceImpl implements EventAuctionService {

    private final EventAuctionItemRepository itemRepository;
    private final EventAuctionBidRepository bidRepository;
    private final CommunityEventRepository eventRepository;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    @Transactional
    public List<EventAuctionItemResponse> getItems(Long communityId, Long eventId) {
        List<EventAuctionItem> items;
        if (eventId != null) {
            items = itemRepository.findByCommunityIdAndEventIdOrderBySortOrderAscIdAsc(communityId, eventId);
        } else {
            items = itemRepository.findByCommunityIdOrderBySortOrderAscIdAsc(communityId);
        }

        // Auto-seed default sample auction items if empty
        if (items.isEmpty() && itemRepository.countByCommunityId(communityId) == 0) {
            Community community = Community.builder().id(communityId).build();
            items = seedDefaultAuctionItems(community, eventId);
        }

        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventAuctionItemResponse getItem(Long id, Long communityId) {
        EventAuctionItem item = itemRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("EventAuctionItem", id));
        return toResponse(item);
    }

    @Override
    @Transactional
    public EventAuctionItemResponse createItem(EventAuctionItemRequest req, AppUser user, Community community) {
        CommunityEvent event = null;
        if (req.getEventId() != null) {
            event = eventRepository.findById(req.getEventId()).orElse(null);
        }

        ItemStatus status = ItemStatus.UPCOMING;
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try {
                status = ItemStatus.valueOf(req.getStatus().trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        BigDecimal basePrice = req.getBasePrice() != null ? req.getBasePrice() : BigDecimal.ZERO;
        BigDecimal minInc = req.getMinIncrement() != null && req.getMinIncrement().compareTo(BigDecimal.ZERO) > 0
                ? req.getMinIncrement() : new BigDecimal("500");

        EventAuctionItem item = EventAuctionItem.builder()
                .community(community)
                .event(event)
                .name(req.getName().trim())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory().trim() : "General")
                .basePrice(basePrice)
                .currentBid(BigDecimal.ZERO)
                .minIncrement(minInc)
                .imageEmoji(req.getImageEmoji() != null ? req.getImageEmoji().trim() : "🪔")
                .imageUrl(req.getImageUrl())
                .status(status)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .bidCount(0)
                .leaderName(null)
                .build();

        EventAuctionItem saved = itemRepository.save(item);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public EventAuctionItemResponse updateItem(Long id, EventAuctionItemRequest req, Long communityId) {
        EventAuctionItem item = itemRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("EventAuctionItem", id));

        if (req.getEventId() != null) {
            CommunityEvent event = eventRepository.findById(req.getEventId()).orElse(null);
            item.setEvent(event);
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            item.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            item.setDescription(req.getDescription());
        }
        if (req.getCategory() != null) {
            item.setCategory(req.getCategory().trim());
        }
        if (req.getBasePrice() != null) {
            item.setBasePrice(req.getBasePrice());
        }
        if (req.getMinIncrement() != null && req.getMinIncrement().compareTo(BigDecimal.ZERO) > 0) {
            item.setMinIncrement(req.getMinIncrement());
        }
        if (req.getImageEmoji() != null) {
            item.setImageEmoji(req.getImageEmoji().trim());
        }
        if (req.getImageUrl() != null) {
            item.setImageUrl(req.getImageUrl());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try {
                ItemStatus newStatus = ItemStatus.valueOf(req.getStatus().trim().toUpperCase());
                item.setStatus(newStatus);
                if (newStatus == ItemStatus.CLOSED && item.getClosedAt() == null) {
                    item.setClosedAt(LocalDateTime.now());
                }
            } catch (Exception ignored) {}
        }
        if (req.getSortOrder() != null) {
            item.setSortOrder(req.getSortOrder());
        }

        EventAuctionItem updated = itemRepository.save(item);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteItem(Long id, Long communityId) {
        EventAuctionItem item = itemRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("EventAuctionItem", id));
        itemRepository.delete(item);
    }

    @Override
    @Transactional
    public EventAuctionItemResponse placeBid(Long itemId, EventAuctionBidRequest bidReq, AppUser user, Community community) {
        EventAuctionItem item = itemRepository.findByIdAndCommunityId(itemId, community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("EventAuctionItem", itemId));

        if (item.getStatus() == ItemStatus.CLOSED) {
            throw new IllegalStateException("This auction item is closed. No further bids are accepted.");
        }

        BigDecimal bidAmount = bidReq.getAmount();
        BigDecimal minRequired;
        if (item.getBidCount() == 0 || item.getCurrentBid().compareTo(BigDecimal.ZERO) == 0) {
            minRequired = item.getBasePrice();
        } else {
            BigDecimal inc = item.getMinIncrement() != null ? item.getMinIncrement() : new BigDecimal("500");
            minRequired = item.getCurrentBid().add(inc);
        }

        if (bidAmount.compareTo(minRequired) < 0) {
            throw new IllegalArgumentException("Bid amount ₹" + bidAmount + " is lower than minimum required ₹" + minRequired);
        }

        String bidderName = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName().trim()
                : (user != null && user.getEmail() != null ? user.getEmail() : "Community Devotee");

        EventAuctionBid bid = EventAuctionBid.builder()
                .item(item)
                .community(community)
                .event(item.getEvent())
                .bidderUserId(user != null ? user.getId() : null)
                .bidderName(bidderName)
                .amount(bidAmount)
                .build();

        bidRepository.save(bid);

        item.setCurrentBid(bidAmount);
        item.setBidCount(item.getBidCount() + 1);
        item.setLeaderName(bidderName);
        item.setStatus(ItemStatus.LIVE);

        EventAuctionItem updated = itemRepository.save(item);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventAuctionBidResponse> getBids(Long itemId, Long communityId) {
        return bidRepository.findByItemIdOrderByBidAtDesc(itemId).stream()
                .map(this::toBidResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventAuctionBidResponse> getRecentBids(Long communityId) {
        return bidRepository.findTop20ByCommunityIdOrderByBidAtDesc(communityId).stream()
                .map(this::toBidResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventAuctionStatsResponse getStats(Long communityId, Long eventId) {
        List<EventAuctionItem> items;
        if (eventId != null) {
            items = itemRepository.findByCommunityIdAndEventIdOrderBySortOrderAscIdAsc(communityId, eventId);
        } else {
            items = itemRepository.findByCommunityIdOrderBySortOrderAscIdAsc(communityId);
        }

        BigDecimal totalRevenue = items.stream()
                .map(EventAuctionItem::getCurrentBid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.size();
        int liveCount = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.LIVE).count();
        int closedCount = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.CLOSED).count();
        int upcomingCount = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.UPCOMING).count();
        int totalBids = items.stream().mapToInt(EventAuctionItem::getBidCount).sum();

        List<Object[]> rawLeaderboard = bidRepository.findLeaderboardByCommunity(communityId);
        List<EventAuctionStatsResponse.EventAuctionLeaderboardEntry> leaderboard = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rawLeaderboard) {
            String name = (String) row[0];
            BigDecimal total = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : BigDecimal.valueOf(((Number) row[1]).doubleValue());
            int count = ((Number) row[2]).intValue();
            leaderboard.add(EventAuctionStatsResponse.EventAuctionLeaderboardEntry.builder()
                    .rank(rank++)
                    .name(name)
                    .totalAmount(total)
                    .bidCount(count)
                    .build());
            if (rank > 10) break;
        }

        return EventAuctionStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalItems(totalItems)
                .liveItemsCount(liveCount)
                .closedItemsCount(closedCount)
                .upcomingItemsCount(upcomingCount)
                .totalBidsCount(totalBids)
                .leaderboard(leaderboard)
                .build();
    }

    private List<EventAuctionItem> seedDefaultAuctionItems(Community community, Long eventId) {
        CommunityEvent event = eventId != null ? eventRepository.findById(eventId).orElse(null) : null;
        List<EventAuctionItem> seed = List.of(
                EventAuctionItem.builder()
                        .community(community).event(event).name("Ganesh Maha Laddu (21 kg)").category("Prasadam")
                        .description("Sacred festival 21kg Ganesh Laddu prasadam blessed during Maha Aarti")
                        .basePrice(new BigDecimal("5000")).currentBid(new BigDecimal("28000")).minIncrement(new BigDecimal("1000"))
                        .imageEmoji("🪔").status(ItemStatus.LIVE).sortOrder(1).bidCount(12).leaderName("Venkat R.").build(),
                EventAuctionItem.builder()
                        .community(community).event(event).name("Pattu Vastram – Silk Dhoti & Shawl").category("Clothing")
                        .description("Handwoven pure silk vastram offered to deity during Kalyanam")
                        .basePrice(new BigDecimal("3000")).currentBid(new BigDecimal("11500")).minIncrement(new BigDecimal("500"))
                        .imageEmoji("𥻻").status(ItemStatus.LIVE).sortOrder(2).bidCount(7).leaderName("Suresh K.").build(),
                EventAuctionItem.builder()
                        .community(community).event(event).name("Silver Padaraksha (Pair)").category("Jewellery")
                        .description("Silver ornamental holy padaraksha pair consecrated during pooja")
                        .basePrice(new BigDecimal("8000")).currentBid(new BigDecimal("22000")).minIncrement(new BigDecimal("1000"))
                        .imageEmoji("🌸").status(ItemStatus.LIVE).sortOrder(3).bidCount(9).leaderName("Ramesh M.").build(),
                EventAuctionItem.builder()
                        .community(community).event(event).name("Gold-Plated Sacred Coconut").category("Ritual")
                        .description("Gold-plated holy coconut consecrated during Navaratri Kalasha Pooja")
                        .basePrice(new BigDecimal("4000")).currentBid(new BigDecimal("15000")).minIncrement(new BigDecimal("500"))
                        .imageEmoji("🥥").status(ItemStatus.LIVE).sortOrder(4).bidCount(5).leaderName("Anitha P.").build(),
                EventAuctionItem.builder()
                        .community(community).event(event).name("Flower Decoration Sponsor Lot A").category("Decor")
                        .description("Prime stage & temple arch grand floral decoration sponsorship")
                        .basePrice(new BigDecimal("2000")).currentBid(BigDecimal.ZERO).minIncrement(new BigDecimal("500"))
                        .imageEmoji("🌺").status(ItemStatus.UPCOMING).sortOrder(5).bidCount(0).leaderName(null).build(),
                EventAuctionItem.builder()
                        .community(community).event(event).name("Annadanam – Grand Feast Sponsorship").category("Seva")
                        .description("Complete 1-day Annadanam sponsorship feeding 1,000+ community devotees")
                        .basePrice(new BigDecimal("25000")).currentBid(BigDecimal.ZERO).minIncrement(new BigDecimal("2000"))
                        .imageEmoji("🍛").status(ItemStatus.UPCOMING).sortOrder(6).bidCount(0).leaderName(null).build()
        );
        return itemRepository.saveAll(seed);
    }

    private EventAuctionItemResponse toResponse(EventAuctionItem item) {
        return EventAuctionItemResponse.builder()
                .id(item.getId())
                .eventId(item.getEvent() != null ? item.getEvent().getId() : null)
                .eventTitle(item.getEvent() != null ? item.getEvent().getTitle() : null)
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory())
                .basePrice(item.getBasePrice())
                .currentBid(item.getCurrentBid())
                .minIncrement(item.getMinIncrement())
                .imageEmoji(item.getImageEmoji())
                .imageUrl(item.getImageUrl())
                .status(item.getStatus() != null ? item.getStatus().name() : ItemStatus.UPCOMING.name())
                .sortOrder(item.getSortOrder())
                .bidCount(item.getBidCount())
                .leaderName(item.getLeaderName())
                .closedAt(item.getClosedAt() != null ? item.getClosedAt().format(ISO_FMT) : null)
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().format(ISO_FMT) : null)
                .build();
    }

    private EventAuctionBidResponse toBidResponse(EventAuctionBid bid) {
        String timeAgo = formatTimeAgo(bid.getBidAt());
        return EventAuctionBidResponse.builder()
                .id(bid.getId())
                .itemId(bid.getItem() != null ? bid.getItem().getId() : null)
                .itemName(bid.getItem() != null ? bid.getItem().getName() : null)
                .eventId(bid.getEvent() != null ? bid.getEvent().getId() : null)
                .bidderUserId(bid.getBidderUserId())
                .bidderName(bid.getBidderName())
                .amount(bid.getAmount())
                .bidAt(bid.getBidAt() != null ? bid.getBidAt().format(ISO_FMT) : null)
                .timeAgo(timeAgo)
                .build();
    }

    private String formatTimeAgo(LocalDateTime dt) {
        if (dt == null) return "just now";
        Duration d = Duration.between(dt, LocalDateTime.now());
        long mins = d.toMinutes();
        if (mins < 1) return "just now";
        if (mins < 60) return mins + "m ago";
        long hours = d.toHours();
        if (hours < 24) return hours + "h ago";
        long days = d.toDays();
        return days + "d ago";
    }
}
