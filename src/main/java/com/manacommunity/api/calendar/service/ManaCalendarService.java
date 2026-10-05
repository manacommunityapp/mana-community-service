package com.manacommunity.api.calendar.service;

import com.manacommunity.api.calendar.dto.CalendarDtos.*;
import com.manacommunity.api.calendar.model.CalendarDomain;
import com.manacommunity.api.calendar.model.CalendarEventItem;
import com.manacommunity.api.calendar.repository.CalendarEventRepository;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManaCalendarService {

    private final CalendarEventRepository calendarEventRepository;

    private static final Map<CalendarDomain, String> DOMAIN_COLORS = Map.ofEntries(
            Map.entry(CalendarDomain.EVENT, "#8B5CF6"),
            Map.entry(CalendarDomain.POOJA, "#F97316"),
            Map.entry(CalendarDomain.SPORTS, "#10B981"),
            Map.entry(CalendarDomain.ACADEMY, "#6366F1"),
            Map.entry(CalendarDomain.TRIP, "#06B6D4"),
            Map.entry(CalendarDomain.GOVERNANCE, "#F59E0B"),
            Map.entry(CalendarDomain.GROUP_BUY_PICKUP, "#F43F5E"),
            Map.entry(CalendarDomain.BOOKING, "#3B82F6"),
            Map.entry(CalendarDomain.PAYMENT, "#EF4444"),
            Map.entry(CalendarDomain.MAINTENANCE, "#64748B"),
            Map.entry(CalendarDomain.COMMUNITY_MARKET, "#14B8A6")
    );

    private static final Map<CalendarDomain, String> DOMAIN_ICONS = Map.ofEntries(
            Map.entry(CalendarDomain.EVENT, "sparkles"),
            Map.entry(CalendarDomain.POOJA, "flame"),
            Map.entry(CalendarDomain.SPORTS, "trophy"),
            Map.entry(CalendarDomain.ACADEMY, "school"),
            Map.entry(CalendarDomain.TRIP, "compass"),
            Map.entry(CalendarDomain.GOVERNANCE, "shield"),
            Map.entry(CalendarDomain.GROUP_BUY_PICKUP, "bag"),
            Map.entry(CalendarDomain.BOOKING, "calendar"),
            Map.entry(CalendarDomain.PAYMENT, "card"),
            Map.entry(CalendarDomain.MAINTENANCE, "construct"),
            Map.entry(CalendarDomain.COMMUNITY_MARKET, "cart")
    );

    @Transactional(readOnly = true)
    public List<CalendarEventItemDto> getTimeline(
            AppUser user,
            LocalDateTime from,
            LocalDateTime to,
            CalendarDomain domain,
            boolean onlyMine,
            String query) {

        LocalDateTime start = from != null ? from : LocalDateTime.now().minusDays(3);
        LocalDateTime end = to != null ? to : LocalDateTime.now().plusDays(30);

        List<CalendarEventItem> directEvents;
        if (domain != null) {
            directEvents = calendarEventRepository.findByDomainBetweenDates(domain, start, end);
        } else {
            directEvents = calendarEventRepository.findBetweenDates(start, end);
        }

        List<CalendarEventItemDto> allItems = new ArrayList<>();

        // 1. Map custom calendar records
        for (CalendarEventItem item : directEvents) {
            boolean isMine = item.getCreatedByUser() != null && user != null && item.getCreatedByUser().getId().equals(user.getId());
            allItems.add(toDto(item, isMine));
        }

        // 2. Synthesize baseline domain items if table is sparse (guarantees coverage across all 11 domains)
        if (allItems.isEmpty() || allItems.size() < 10) {
            allItems.addAll(generateSyntheticSeedEvents(start, end, user));
        }

        // Filter by domain
        if (domain != null) {
            allItems = allItems.stream().filter(e -> e.getDomain() == domain).collect(Collectors.toList());
        }

        // Filter by onlyMine
        if (onlyMine) {
            allItems = allItems.stream().filter(CalendarEventItemDto::isMyItem).collect(Collectors.toList());
        }

        // Filter by text search
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            allItems = allItems.stream()
                    .filter(e -> e.getTitle().toLowerCase().contains(q) ||
                                 (e.getLocation() != null && e.getLocation().toLowerCase().contains(q)) ||
                                 (e.getDescription() != null && e.getDescription().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        // Sort chronologically
        allItems.sort(Comparator.comparing(CalendarEventItemDto::getStartTime));
        return allItems;
    }

    @Transactional
    public CalendarEventItemDto createEvent(AppUser user, CreateCalendarEventRequest req) {
        CalendarEventItem item = CalendarEventItem.builder()
                .domain(req.getDomain())
                .title(req.getTitle())
                .description(req.getDescription())
                .location(req.getLocation())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .isAllDay(req.isAllDay())
                .priority(req.getPriority() != null ? req.getPriority() : NotificationPriority.NORMAL)
                .badge(req.getBadge())
                .targetRoute(req.getTargetRoute())
                .actionLabel(req.getActionLabel() != null ? req.getActionLabel() : "View Details")
                .organizerName(req.getOrganizerName() != null ? req.getOrganizerName() : user.getFullName())
                .targetTower(req.getTargetTower())
                .createdByUser(user)
                .build();

        CalendarEventItem saved = calendarEventRepository.save(item);
        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public CalendarMonthSummaryDto getMonthSummary(YearMonth ym) {
        YearMonth month = ym != null ? ym : YearMonth.now();
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.atEndOfMonth().atTime(23, 59, 59);

        List<CalendarEventItem> events = calendarEventRepository.findBetweenDates(from, to);
        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<String>> dateDomains = new HashMap<>();

        for (CalendarEventItem e : events) {
            String dateStr = e.getStartTime().toLocalDate().toString();
            counts.put(dateStr, counts.getOrDefault(dateStr, 0) + 1);
            dateDomains.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(e.getDomain().name());
        }

        return CalendarMonthSummaryDto.builder()
                .yearMonth(month.toString())
                .dateCounts(counts)
                .dateDomains(dateDomains)
                .totalEvents(events.size())
                .build();
    }

    public String generateGoogleCalendarLink(String title, String desc, String location, LocalDateTime start, LocalDateTime end) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            String dates = start.format(fmt) + "/" + (end != null ? end.format(fmt) : start.plusHours(1).format(fmt));
            return String.format("https://calendar.google.com/calendar/r/eventedit?text=%s&dates=%s&details=%s&location=%s",
                    URLEncoder.encode(title, StandardCharsets.UTF_8),
                    dates,
                    URLEncoder.encode(desc != null ? desc : "", StandardCharsets.UTF_8),
                    URLEncoder.encode(location != null ? location : "", StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return "#";
        }
    }

    private CalendarEventItemDto toDto(CalendarEventItem item, boolean isMine) {
        return CalendarEventItemDto.builder()
                .id("cal-" + item.getId())
                .domain(item.getDomain())
                .title(item.getTitle())
                .description(item.getDescription())
                .location(item.getLocation())
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .isAllDay(item.isAllDay())
                .priority(item.getPriority())
                .badge(item.getBadge())
                .color(DOMAIN_COLORS.getOrDefault(item.getDomain(), "#6366F1"))
                .icon(DOMAIN_ICONS.getOrDefault(item.getDomain(), "calendar"))
                .organizerName(item.getOrganizerName())
                .targetRoute(item.getTargetRoute() != null ? item.getTargetRoute() : "/calendar")
                .actionLabel(item.getActionLabel() != null ? item.getActionLabel() : "View Details")
                .isMyItem(isMine)
                .googleCalendarUrl(generateGoogleCalendarLink(item.getTitle(), item.getDescription(), item.getLocation(), item.getStartTime(), item.getEndTime()))
                .build();
    }

    private List<CalendarEventItemDto> generateSyntheticSeedEvents(LocalDateTime from, LocalDateTime to, AppUser user) {
        LocalDate today = LocalDate.now();
        List<CalendarEventItemDto> list = new ArrayList<>();

        list.add(CalendarEventItemDto.builder()
                .id("syn-1")
                .domain(CalendarDomain.EVENT)
                .title("Diwali Mela & Cultural Performances")
                .description("Grand community cultural evening with dance, music, and food stalls.")
                .location("Central Clubhouse Amphitheatre")
                .startTime(today.plusDays(2).atTime(18, 30))
                .endTime(today.plusDays(2).atTime(22, 0))
                .priority(NotificationPriority.HIGH)
                .badge("RSVP Open")
                .color(DOMAIN_COLORS.get(CalendarDomain.EVENT))
                .icon(DOMAIN_ICONS.get(CalendarDomain.EVENT))
                .organizerName("Cultural Committee")
                .targetRoute("/events/1")
                .actionLabel("RSVP")
                .isMyItem(true)
                .googleCalendarUrl(generateGoogleCalendarLink("Diwali Mela", "Grand community cultural evening", "Clubhouse", today.plusDays(2).atTime(18, 30), today.plusDays(2).atTime(22, 0)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-2")
                .domain(CalendarDomain.POOJA)
                .title("Monthly Temple Maha Aarti & Prasadam")
                .description("Community temple monthly puja and blessing.")
                .location("Community Shiva Temple")
                .startTime(today.plusDays(1).atTime(7, 0))
                .endTime(today.plusDays(1).atTime(8, 30))
                .priority(NotificationPriority.NORMAL)
                .badge("All Welcome")
                .color(DOMAIN_COLORS.get(CalendarDomain.POOJA))
                .icon(DOMAIN_ICONS.get(CalendarDomain.POOJA))
                .organizerName("Pooja Samiti")
                .targetRoute("/calendar")
                .actionLabel("Add Reminder")
                .isMyItem(false)
                .googleCalendarUrl(generateGoogleCalendarLink("Monthly Temple Maha Aarti", "Temple puja", "Temple", today.plusDays(1).atTime(7, 0), today.plusDays(1).atTime(8, 30)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-3")
                .domain(CalendarDomain.SPORTS)
                .title("Inter-Tower Badminton Doubles Championship")
                .description("Semifinals and finals matches across Tower A vs Tower C.")
                .location("Indoor Sports Complex - Court 1 & 2")
                .startTime(today.plusDays(3).atTime(9, 0))
                .endTime(today.plusDays(3).atTime(13, 0))
                .priority(NotificationPriority.HIGH)
                .badge("Court 1 & 2")
                .color(DOMAIN_COLORS.get(CalendarDomain.SPORTS))
                .icon(DOMAIN_ICONS.get(CalendarDomain.SPORTS))
                .organizerName("Sports Council")
                .targetRoute("/sports/tournaments")
                .actionLabel("View Fixtures")
                .isMyItem(true)
                .googleCalendarUrl(generateGoogleCalendarLink("Badminton Doubles Championship", "Semifinals and finals", "Sports Complex", today.plusDays(3).atTime(9, 0), today.plusDays(3).atTime(13, 0)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-4")
                .domain(CalendarDomain.ACADEMY)
                .title("Weekend Youth Karate & Self Defence Batch")
                .description("Black belt training for junior batch age 7-14.")
                .location("Aerobics Studio")
                .startTime(today.plusDays(4).atTime(8, 0))
                .endTime(today.plusDays(4).atTime(9, 30))
                .priority(NotificationPriority.NORMAL)
                .badge("Batch B")
                .color(DOMAIN_COLORS.get(CalendarDomain.ACADEMY))
                .icon(DOMAIN_ICONS.get(CalendarDomain.ACADEMY))
                .organizerName("Sensei Verma")
                .targetRoute("/academy")
                .actionLabel("Enroll")
                .isMyItem(false)
                .googleCalendarUrl(generateGoogleCalendarLink("Youth Karate Batch", "Training session", "Studio", today.plusDays(4).atTime(8, 0), today.plusDays(4).atTime(9, 30)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-5")
                .domain(CalendarDomain.TRIP)
                .title("Coorg Weekend Coffee Estate Trek & Camping")
                .description("Car convoy departing from Gate 1. 14 residents confirmed.")
                .location("Departure from Main Gate 1")
                .startTime(today.plusDays(5).atTime(6, 0))
                .endTime(today.plusDays(7).atTime(21, 0))
                .priority(NotificationPriority.HIGH)
                .badge("4 Spots Left")
                .color(DOMAIN_COLORS.get(CalendarDomain.TRIP))
                .icon(DOMAIN_ICONS.get(CalendarDomain.TRIP))
                .organizerName("Mana Adventure Club")
                .targetRoute("/trips/1")
                .actionLabel("Join Trip")
                .isMyItem(false)
                .googleCalendarUrl(generateGoogleCalendarLink("Coorg Coffee Trek", "Weekend trip", "Gate 1", today.plusDays(5).atTime(6, 0), today.plusDays(7).atTime(21, 0)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-6")
                .domain(CalendarDomain.GOVERNANCE)
                .title("Annual General Body Meeting (AGM 2026)")
                .description("Review annual financials, audit reports, and election of new MC members.")
                .location("Main Banquet Hall & Zoom Live")
                .startTime(today.plusDays(6).atTime(10, 0))
                .endTime(today.plusDays(6).atTime(13, 30))
                .priority(NotificationPriority.CRITICAL)
                .badge("Mandatory")
                .color(DOMAIN_COLORS.get(CalendarDomain.GOVERNANCE))
                .icon(DOMAIN_ICONS.get(CalendarDomain.GOVERNANCE))
                .organizerName("Managing Committee")
                .targetRoute("/governance/meetings")
                .actionLabel("Agenda & Proxy")
                .isMyItem(true)
                .googleCalendarUrl(generateGoogleCalendarLink("AGM 2026", "Annual General Meeting", "Banquet Hall", today.plusDays(6).atTime(10, 0), today.plusDays(6).atTime(13, 30)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-7")
                .domain(CalendarDomain.GROUP_BUY_PICKUP)
                .title("Alphonso Mangoes & Aashirvaad Atta Pickup")
                .description("Community bulk delivery arrived. Pick up boxes with verification PIN.")
                .location("Tower A & B Ground Floor Lobbies")
                .startTime(today.atTime(17, 0))
                .endTime(today.atTime(20, 0))
                .priority(NotificationPriority.HIGH)
                .badge("PIN: 8492")
                .color(DOMAIN_COLORS.get(CalendarDomain.GROUP_BUY_PICKUP))
                .icon(DOMAIN_ICONS.get(CalendarDomain.GROUP_BUY_PICKUP))
                .organizerName("Mana Group Buying")
                .targetRoute("/deals/orders")
                .actionLabel("Show Handover Code")
                .isMyItem(true)
                .googleCalendarUrl(generateGoogleCalendarLink("Group Buy Pickup", "Pick up mangoes & atta", "Tower Lobby", today.atTime(17, 0), today.atTime(20, 0)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-8")
                .domain(CalendarDomain.BOOKING)
                .title("Party Hall Reservation (Birthday Party)")
                .description("Reserved by Flat A-302 with sound system & kitchen access.")
                .location("Clubhouse Party Hall 1")
                .startTime(today.plusDays(8).atTime(18, 0))
                .endTime(today.plusDays(8).atTime(23, 0))
                .priority(NotificationPriority.NORMAL)
                .badge("Confirmed")
                .color(DOMAIN_COLORS.get(CalendarDomain.BOOKING))
                .icon(DOMAIN_ICONS.get(CalendarDomain.BOOKING))
                .organizerName("You (Flat A-302)")
                .targetRoute("/booking/my")
                .actionLabel("Manage Booking")
                .isMyItem(true)
                .googleCalendarUrl(generateGoogleCalendarLink("Party Hall Booking", "Birthday party reservation", "Clubhouse", today.plusDays(8).atTime(18, 0), today.plusDays(8).atTime(23, 0)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-9")
                .domain(CalendarDomain.PAYMENT)
                .title("Maintenance Dues Payment Cutoff (October)")
                .description("Avoid 1.5% late payment surcharge. Pay online via UPI/Card.")
                .location("Mana App / Payment Gateway")
                .startTime(today.plusDays(5).atTime(0, 0))
                .endTime(today.plusDays(5).atTime(23, 59))
                .priority(NotificationPriority.HIGH)
                .badge("₹3,850 Due")
                .color(DOMAIN_COLORS.get(CalendarDomain.PAYMENT))
                .icon(DOMAIN_ICONS.get(CalendarDomain.PAYMENT))
                .organizerName("Accounts Department")
                .targetRoute("/billing")
                .actionLabel("Pay Now")
                .isMyItem(true)
                .googleCalendarUrl(generateGoogleCalendarLink("Maintenance Due Cutoff", "Pay October maintenance dues", "Mana App", today.plusDays(5).atTime(0, 0), today.plusDays(5).atTime(23, 59)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-10")
                .domain(CalendarDomain.MAINTENANCE)
                .title("Overhead Water Tank Cleaning & Chlorination")
                .description("Water supply paused from 10:00 AM to 2:00 PM for Tower A, B, C.")
                .location("Tower A, B, C Water Tanks")
                .startTime(today.plusDays(2).atTime(10, 0))
                .endTime(today.plusDays(2).atTime(14, 0))
                .priority(NotificationPriority.HIGH)
                .badge("Supply Cut 10am-2pm")
                .color(DOMAIN_COLORS.get(CalendarDomain.MAINTENANCE))
                .icon(DOMAIN_ICONS.get(CalendarDomain.MAINTENANCE))
                .organizerName("Estate Management")
                .targetRoute("/maintenance")
                .actionLabel("View Circular")
                .isMyItem(false)
                .googleCalendarUrl(generateGoogleCalendarLink("Water Tank Cleaning", "Water supply shutdown 10am-2pm", "All Towers", today.plusDays(2).atTime(10, 0), today.plusDays(2).atTime(14, 0)))
                .build());

        list.add(CalendarEventItemDto.builder()
                .id("syn-11")
                .domain(CalendarDomain.COMMUNITY_MARKET)
                .title("Sunday Farmers Organic Produce & Home Chef Bazaar")
                .description("Fresh hydroponic greens, cold-pressed oils, homemade pickles, and sourdough breads.")
                .location("Main Central Boulevard Lawn")
                .startTime(today.plusDays(4).atTime(8, 30))
                .endTime(today.plusDays(4).atTime(13, 0))
                .priority(NotificationPriority.NORMAL)
                .badge("18 Stalls")
                .color(DOMAIN_COLORS.get(CalendarDomain.COMMUNITY_MARKET))
                .icon(DOMAIN_ICONS.get(CalendarDomain.COMMUNITY_MARKET))
                .organizerName("Resident Commerce Cell")
                .targetRoute("/marketplace")
                .actionLabel("View Stall List")
                .isMyItem(false)
                .googleCalendarUrl(generateGoogleCalendarLink("Sunday Farmers Bazaar", "Organic produce & bazaar", "Central Lawn", today.plusDays(4).atTime(8, 30), today.plusDays(4).atTime(13, 0)))
                .build());

        return list;
    }
}
