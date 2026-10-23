package com.manacommunity.api.controller;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api/v1/experiences")
@CrossOrigin(origins = "*")
public class ExperiencesController {

    // ── DTOs ──────────────────────────────────────────────────────────────────

    record Experience(
        String id, String title, String category, String emoji, String backdropColor,
        String description, String organizerName, boolean organizerVerified,
        String venue, String venueArea, String startDate, String startTime,
        Integer durationMins, boolean isFree, Integer priceIndividual, Integer priceGroup,
        Integer groupMinSize, Integer groupCurrentSize, String groupDeadline,
        String visibility, String communityTag, int capacity, int registeredCount,
        String status, boolean isFeatured, List<String> tags, String ageSuitability,
        String refundPolicy
    ) {}

    record Booking(
        String bookingId, String experienceId, String experienceTitle,
        String category, String emoji, String backdropColor,
        String ticketType, int attendeeCount, double totalAmount,
        double originalAmount, double discountAmount, String couponCode,
        String status, String bookedAt, String qrData,
        String refundStatus, List<Map<String, Object>> familyMembers
    ) {}

    record BookRequest(
        String experienceId, String name, String phone, int attendeeCount,
        String ticketType, String couponCode, List<Map<String, Object>> familyMembers
    ) {}

    record BookResult(
        String bookingId, String upiIntentUrl, double totalAmount, double discountAmount,
        String couponCode, String qrData, String status
    ) {}

    record CouponResult(
        boolean valid, String code, String discountType, double discountValue,
        double discountAmount, double finalAmount, String message
    ) {}

    record WaitlistEntry(
        String waitlistId, String experienceId, int position,
        String name, String phone, String joinedAt
    ) {}

    record RefundRequest(
        String refundId, String bookingId, String reason, double amountRequested,
        String status, String requestedAt, int estimatedDays
    ) {}

    record OrganizerReport(
        String experienceId, String experienceTitle, String emoji, String backdropColor,
        String startDate, String venue, int capacity, int registeredCount, int checkedInCount,
        double grossRevenue, double platformFee, double netSettlement, double refundsTotal,
        List<Map<String, Object>> ticketBreakdown, int couponUsageCount,
        List<Map<String, Object>> attendees
    ) {}

    record Partner(
        String id, String name, String category, String emoji, String backdropColor,
        String tagline, String description, String address, String area,
        double rating, int reviewCount, boolean isVerified, boolean isFeatured,
        String discountForMembers, String contactPhone, String bookingUrl,
        List<String> amenities, String openHours, String priceRange,
        List<String> experienceIds
    ) {}

    record CoachingSession(
        String sessionId, String partnerId, String partnerName, String sport, String emoji,
        String coachName, String coachBio, String level,
        int durationMins, int pricePerSession, int groupSizeMax,
        List<Map<String, Object>> availableSlots, String venue, String area
    ) {}

    record Review(
        String reviewId, String experienceId, String reviewerName, String reviewerInitials,
        double rating, String title, String body, String attendedDate,
        String createdAt, int helpfulCount, boolean verifiedAttendee, int photosCount
    ) {}

    record ReviewRequest(
        String experienceId, String bookingId, String reviewerName, double rating, String title, String body
    ) {}

    record InterestMatch(
        String residentName, String residentInitials, String flat,
        List<String> sharedInterests, int matchScore, boolean openToGroup
    ) {}

    record ItineraryItem(
        String experienceId, String experienceTitle, String emoji, String backdropColor,
        String date, String startTime, String venue, String venueArea,
        Double price, boolean isFree, String notes
    ) {}

    record BuddyMessage(
        String id, String role, String text, String createdAt
    ) {}

    // ── Mock Data ─────────────────────────────────────────────────────────────

    private static final List<Experience> MOCK_EXPERIENCES = List.of(
        new Experience("exp-001", "Morning Yoga & Meditation", "WORKSHOPS", "🧘", "#059669",
            "Start your day with peaceful yoga and guided meditation in the community garden.",
            "Priya Wellness", true, "Club Lawn", "Phase 1",
            LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE), "07:00",
            60, true, null, null, null, null, null,
            "OPEN", null, 25, 18, "UPCOMING", true,
            List.of("yoga", "wellness", "morning"), "All ages", "Non-refundable"),
        new Experience("exp-002", "Photography Walk: Golden Hour", "OUTINGS", "📸", "#D97706",
            "Join our photography club for a stunning golden-hour walk around the community gardens.",
            "Lens Club Mana", false, "Garden Walk", "East Wing",
            LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE), "06:30",
            90, false, 150, null, null, null, null,
            "OPEN", null, 20, 12, "UPCOMING", false,
            List.of("photography", "outdoors", "creative"), "12+", "Refundable till 24h before"),
        new Experience("exp-003", "Community Cricket T20", "SPORTS", "🏏", "#0891B2",
            "Gear up for a thrilling T20 cricket match between community blocks. Full gear provided!",
            "Sports Committee", true, "Sports Complex", "Block D",
            LocalDate.now().plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE), "10:00",
            180, false, 150, 120, 8, 14, null,
            "OPEN", null, 44, 28, "UPCOMING", true,
            List.of("cricket", "sports", "team"), "15+", "Refundable till 48h before"),
        new Experience("exp-004", "Bollywood Movie Night", "MOVIES", "🎬", "#7C3AED",
            "Outdoor screening of the blockbuster hit under the stars with popcorn and snacks.",
            "Entertainment Club", true, "Community Hall", "Main Block",
            LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE), "19:30",
            180, false, 200, null, null, null, null,
            "OPEN", null, 100, 67, "UPCOMING", true,
            List.of("movies", "entertainment", "family"), "All ages", "Non-refundable"),
        new Experience("exp-005", "North Indian Cooking Workshop", "WORKSHOPS", "👨‍🍳", "#DB2777",
            "Learn authentic North Indian recipes from a professional chef. Ingredients included!",
            "Chef Anita Kumar", true, "Community Kitchen", "Block B",
            LocalDate.now().plusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE), "11:00",
            150, false, 350, null, null, null, null,
            "MEMBERS_ONLY", "members", 16, 16, "UPCOMING", false,
            List.of("cooking", "food", "workshop"), "18+", "Refundable till 48h before"),
        new Experience("exp-006", "Kids Art & Craft Fiesta", "KIDS_FAMILY", "🎨", "#F59E0B",
            "A fun-filled art session for kids with expert instructors. All materials provided!",
            "Little Artists Club", false, "Activity Hall", "Block A",
            LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE), "10:30",
            120, false, 250, 800, 3, null, null,
            "OPEN", null, 30, 22, "UPCOMING", false,
            List.of("kids", "art", "family"), "4-12 yrs", "Refundable till 24h before")
    );

    private static final List<Partner> MOCK_PARTNERS = List.of(
        new Partner("p-001", "Chaitime Café", "RESTAURANT", "☕", "#92400E",
            "Premium chai & snacks — 20% off for residents", "Artisanal chai and fresh snacks",
            "Block A Ground Floor", "Phase 1", 4.5, 128, true, true,
            "20% off all orders", "9876543210", null,
            List.of("Dine-in", "Takeaway", "Home Delivery"), "7am – 10pm", "₹₹", null),
        new Partner("p-002", "AquaArena Swim Club", "SPORTS_VENUE", "🏊", "#0891B2",
            "Olympic-size pool · Members swim free daily",
            "Professional swimming facility with heated pool",
            "Sports Complex, Block D", "Block D", 4.8, 234, true, true,
            "Members swim free 6-8am & 6-8pm", "9876543211", null,
            List.of("Heated Pool", "Lockers", "Coaching", "Kids Pool"), "5am – 10pm", "₹₹₹", List.of("exp-001")),
        new Partner("p-003", "Elite Cricket Academy", "COACHING", "🏏", "#059669",
            "BCCI-certified coaches · Batch training", "Professional cricket coaching for all levels",
            "Cricket Ground, Block D", "Block D", 4.7, 189, true, false,
            "15% off monthly packages", "9876543212", null,
            List.of("Nets", "Video Analysis", "Gear Rental", "All Levels"), "6am – 8pm", "₹₹₹", null),
        new Partner("p-004", "MindBody Wellness", "WELLNESS", "🧘", "#7C3AED",
            "Yoga · Meditation · Therapy", "Holistic wellness center",
            "Block C, 2nd Floor", "Block C", 4.6, 156, false, false,
            "10% off first session", "9876543213", null,
            List.of("Yoga Studio", "Meditation Room", "Therapy", "Online Classes"), "6am – 9pm", "₹₹", null)
    );

    private static final List<CoachingSession> MOCK_COACHING = List.of(
        new CoachingSession("cs-001", "p-003", "Elite Cricket Academy", "Cricket", "🏏",
            "Coach Ravi Kumar", "Ex-Ranji Trophy player, 10 years coaching experience",
            "ALL", 90, 800, 8,
            List.of(
                Map.of("date", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE), "time", "07:00", "spotsLeft", 3),
                Map.of("date", LocalDate.now().plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE), "time", "07:00", "spotsLeft", 5)
            ),
            "Cricket Ground", "Block D"),
        new CoachingSession("cs-002", "p-002", "AquaArena Swim Club", "Swimming", "🏊",
            "Coach Preethi Sharma", "National-level swimmer, specialises in competitive technique",
            "BEGINNER", 60, 600, 6,
            List.of(
                Map.of("date", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE), "time", "06:00", "spotsLeft", 2)
            ),
            "AquaArena Pool", "Block D"),
        new CoachingSession("cs-003", "p-004", "MindBody Wellness", "Yoga", "🧘",
            "Instructor Meera Iyer", "RYT-500 certified, 8 years teaching Hatha & Vinyasa",
            "ALL", 75, 500, 10,
            List.of(
                Map.of("date", LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE), "time", "07:00", "spotsLeft", 4),
                Map.of("date", LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE), "time", "07:00", "spotsLeft", 8)
            ),
            "MindBody Studio", "Block C")
    );

    private static final List<Review> MOCK_REVIEWS = List.of(
        new Review("rv-001", "exp-001", "Anjali Sharma", "AS", 5.0,
            "Best morning routine ever!", "Absolutely loved the session. Priya is an amazing instructor.",
            LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE),
            LocalDate.now().minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE),
            12, true, 2),
        new Review("rv-002", "exp-001", "Ravi Nair", "RN", 4.0,
            "Very relaxing and well-organised",
            "Great experience overall. The garden setting made it even more peaceful.",
            LocalDate.now().minusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE),
            LocalDate.now().minusDays(13).format(DateTimeFormatter.ISO_LOCAL_DATE),
            8, true, 0),
        new Review("rv-003", "exp-003", "Karthik Reddy", "KR", 5.0,
            "Epic cricket match!",
            "Best community event I've attended. Competitive yet friendly atmosphere.",
            LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE),
            LocalDate.now().minusDays(9).format(DateTimeFormatter.ISO_LOCAL_DATE),
            19, true, 3)
    );

    private static final List<InterestMatch> MOCK_MATCHES = List.of(
        new InterestMatch("Arjun Mehta", "AM", "A-304", List.of("CRICKET", "BADMINTON", "CYCLING"), 92, true),
        new InterestMatch("Priya Sharma", "PS", "B-102", List.of("YOGA", "MEDITATION", "PHOTOGRAPHY"), 88, true),
        new InterestMatch("Rohit Nair", "RN", "C-501", List.of("FOOTBALL", "GAMING", "MOVIES"), 81, false),
        new InterestMatch("Kavya Reddy", "KR", "A-207", List.of("DANCE", "MUSIC", "COOKING"), 76, true),
        new InterestMatch("Sanjay Iyer", "SI", "D-403", List.of("TREKKING", "CYCLING", "PHOTOGRAPHY"), 71, true)
    );

    private static final Map<String, String> COUPON_CODES = Map.of(
        "MANA10",  "PERCENT:10",
        "FLAT200", "FLAT:200",
        "SAVE50",  "PERCENT:50",
        "WELCOME", "FLAT:100"
    );

    // ── Experiences ───────────────────────────────────────────────────────────

    @GetMapping
    public List<Experience> getExperiences(
        @RequestParam(required = false) Boolean featured,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String query
    ) {
        return MOCK_EXPERIENCES.stream()
            .filter(e -> featured == null || !featured || e.isFeatured())
            .filter(e -> category == null || e.category().equalsIgnoreCase(category))
            .filter(e -> query == null || e.title().toLowerCase().contains(query.toLowerCase())
                || e.tags().stream().anyMatch(t -> t.toLowerCase().contains(query.toLowerCase())))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getExperience(@PathVariable String id) {
        return MOCK_EXPERIENCES.stream()
            .filter(e -> e.id().equals(id))
            .findFirst()
            .map(e -> Map.<String, Object>of("experience", e))
            .orElse(Map.<String, Object>of("error", "Not found"));
    }

    // ── Booking ───────────────────────────────────────────────────────────────

    @PostMapping("/book")
    public BookResult book(@RequestBody BookRequest req) {
        String bookingId = "EXP-" + System.currentTimeMillis();
        double base = req.attendeeCount() * 200.0;
        double discount = 0;
        if (req.couponCode() != null && COUPON_CODES.containsKey(req.couponCode().toUpperCase())) {
            String[] parts = COUPON_CODES.get(req.couponCode().toUpperCase()).split(":");
            discount = parts[0].equals("PERCENT")
                ? base * Double.parseDouble(parts[1]) / 100
                : Double.parseDouble(parts[1]);
        }
        double total = Math.max(0, base - discount);
        String upi = "upi://pay?pa=manaexperiences@ybl&pn=ManaExperiences&am=" + total
                   + "&cu=INR&tn=EXP+" + bookingId;
        return new BookResult(bookingId, upi, total, discount,
            req.couponCode(), "EXP-QR-" + bookingId, "CONFIRMED");
    }

    @GetMapping("/my-bookings")
    public List<Booking> getMyBookings() {
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 3 && i < MOCK_EXPERIENCES.size(); i++) {
            Experience e = MOCK_EXPERIENCES.get(i);
            bookings.add(new Booking(
                "EXP-" + (1001 + i), e.id(), e.title(), e.category(), e.emoji(), e.backdropColor(),
                "GENERAL", 2, e.priceIndividual() != null ? e.priceIndividual() * 2.0 : 0,
                e.priceIndividual() != null ? e.priceIndividual() * 2.0 : 0, 0, null,
                "CONFIRMED", LocalDate.now().minusDays(i + 1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                "EXP-QR-EXP-" + (1001 + i), "NONE", null
            ));
        }
        return bookings;
    }

    // ── P2: Commerce ──────────────────────────────────────────────────────────

    @PostMapping("/coupons/validate")
    public CouponResult validateCoupon(@RequestBody Map<String, Object> body) {
        String code = ((String) body.getOrDefault("code", "")).toUpperCase();
        double original = ((Number) body.getOrDefault("originalAmount", 500)).doubleValue();
        if (!COUPON_CODES.containsKey(code)) {
            return new CouponResult(false, code, null, 0, 0, original, "Invalid coupon code");
        }
        String[] parts = COUPON_CODES.get(code).split(":");
        boolean isPercent = parts[0].equals("PERCENT");
        double value = Double.parseDouble(parts[1]);
        double discountAmt = isPercent ? original * value / 100 : Math.min(value, original);
        double finalAmt = Math.max(0, original - discountAmt);
        String msg = isPercent ? value + "% off applied!" : "₹" + (int)value + " off applied!";
        return new CouponResult(true, code, parts[0], value, discountAmt, finalAmt, msg);
    }

    @PostMapping("/waitlist")
    public WaitlistEntry joinWaitlist(@RequestBody Map<String, Object> body) {
        return new WaitlistEntry(
            "WL-" + System.currentTimeMillis(),
            (String) body.getOrDefault("experienceId", "exp-001"),
            (int) (Math.random() * 10 + 1),
            (String) body.getOrDefault("name", "Resident"),
            (String) body.getOrDefault("phone", ""),
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    @PostMapping("/refund")
    public RefundRequest requestRefund(@RequestBody Map<String, Object> body) {
        return new RefundRequest(
            "RFD-" + System.currentTimeMillis(),
            (String) body.getOrDefault("bookingId", ""),
            (String) body.getOrDefault("reason", ""),
            ((Number) body.getOrDefault("amount", 0)).doubleValue(),
            "REQUESTED",
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            5
        );
    }

    @GetMapping("/organizer")
    public List<Experience> getOrganizerExperiences() {
        return MOCK_EXPERIENCES.stream().limit(2).collect(Collectors.toList());
    }

    @GetMapping("/organizer/{id}/report")
    public OrganizerReport getOrganizerReport(@PathVariable String id) {
        Experience exp = MOCK_EXPERIENCES.stream()
            .filter(e -> e.id().equals(id))
            .findFirst()
            .orElse(MOCK_EXPERIENCES.get(0));
        double revenue = exp.priceIndividual() != null ? exp.registeredCount() * exp.priceIndividual() : 0;
        double fee = revenue * 0.05;
        List<Map<String, Object>> breakdown = List.of(
            Map.of("type", "GENERAL", "count", exp.registeredCount() - 4, "revenue", (exp.registeredCount() - 4) * (exp.priceIndividual() != null ? exp.priceIndividual() : 0)),
            Map.of("type", "FAMILY", "count", 2, "revenue", 2 * (exp.priceIndividual() != null ? exp.priceIndividual() * 3 : 0)),
            Map.of("type", "GROUP", "count", 1, "revenue", exp.priceGroup() != null ? exp.priceGroup() : 0)
        );
        List<Map<String, Object>> attendees = new ArrayList<>();
        String[] names = {"Rahul K", "Priya S", "Ananya R", "Vikram M", "Deepa N"};
        String[] phones = {"98765-43210", "98765-43211", "98765-43212", "98765-43213", "98765-43214"};
        for (int i = 0; i < 5; i++) {
            attendees.add(Map.of(
                "bookingId", "EXP-" + (2001 + i),
                "name", names[i], "phone", phones[i],
                "ticketType", "GENERAL", "attendeeCount", 2,
                "amountPaid", exp.priceIndividual() != null ? exp.priceIndividual() * 2 : 0,
                "status", "CONFIRMED", "bookedAt", LocalDate.now().minusDays(i + 1).toString(),
                "checkedIn", i < 3, "refundStatus", "NONE"
            ));
        }
        return new OrganizerReport(
            exp.id(), exp.title(), exp.emoji(), exp.backdropColor(),
            exp.startDate(), exp.venue(), exp.capacity(), exp.registeredCount(), (int)(exp.registeredCount() * 0.6),
            revenue, fee, revenue - fee, 0,
            breakdown, 3, attendees
        );
    }

    // ── P3: Partners & Coaching ───────────────────────────────────────────────

    @GetMapping("/partners")
    public List<Partner> getPartners(@RequestParam(required = false) String category) {
        return MOCK_PARTNERS.stream()
            .filter(p -> category == null || p.category().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    @GetMapping("/coaching")
    public List<CoachingSession> getCoachingSessions(@RequestParam(required = false) String sport) {
        return MOCK_COACHING.stream()
            .filter(s -> sport == null || s.sport().toLowerCase().contains(sport.toLowerCase()))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}/reviews")
    public List<Review> getReviews(@PathVariable String id) {
        return MOCK_REVIEWS.stream()
            .filter(r -> r.experienceId().equals(id))
            .collect(Collectors.toList());
    }

    @PostMapping("/reviews")
    public Review submitReview(@RequestBody ReviewRequest req) {
        String reviewerName = req.reviewerName() != null && !req.reviewerName().isBlank() ? req.reviewerName().trim() : "Resident";
        String[] parts = reviewerName.split("\\s+");
        String initials = parts.length >= 2
            ? parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)
            : (reviewerName.length() >= 2 ? reviewerName.substring(0, 2) : "RR");
        return new Review(
            "rv-" + System.currentTimeMillis(),
            req.experienceId(), reviewerName,
            initials.toUpperCase(), req.rating(), req.title(), req.body(),
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            0, true, 0
        );
    }

    // ── P4: Mana Buddy AI ─────────────────────────────────────────────────────

    @GetMapping("/buddy/interests")
    public List<String> getUserInterests() {
        return List.of("CRICKET", "MOVIES", "YOGA");
    }

    @PutMapping("/buddy/interests")
    public Map<String, Object> saveUserInterests(@RequestBody Map<String, Object> body) {
        return Map.of("success", true, "message", "Interests saved");
    }

    @PostMapping("/buddy/recommendations")
    public List<Map<String, Object>> getBuddyRecommendations(@RequestBody Map<String, Object> body) {
        List<String> interests = (List<String>) body.getOrDefault("interests", List.of());
        Map<String, String> tagToCategory = Map.of(
            "CRICKET", "SPORTS", "SWIMMING", "SPORTS", "BADMINTON", "SPORTS",
            "YOGA", "WORKSHOPS", "FITNESS", "WORKSHOPS", "DANCE", "WORKSHOPS",
            "MOVIES", "MOVIES", "MUSIC", "LIVE_EVENTS",
            "COOKING", "WORKSHOPS", "TREKKING", "OUTINGS"
        );
        Set<String> categories = interests.stream()
            .map(i -> tagToCategory.getOrDefault(i, ""))
            .filter(c -> !c.isEmpty())
            .collect(Collectors.toSet());
        Map<String, String> reasons = Map.of(
            "SPORTS", "Matches your sports interest",
            "WORKSHOPS", "Based on your wellness & hobby picks",
            "MOVIES", "Top pick for movie lovers",
            "OUTINGS", "Great for your adventurous spirit"
        );
        return MOCK_EXPERIENCES.stream()
            .filter(e -> categories.isEmpty() || categories.contains(e.category()))
            .limit(5)
            .map(e -> Map.of(
                "experience", (Object) e,
                "reason", reasons.getOrDefault(e.category(), "Recommended for you"),
                "matchScore", (Object) (70 + Math.abs(e.id().hashCode() % 28))
            ))
            .collect(Collectors.toList());
    }

    @PostMapping("/buddy/itinerary")
    public List<ItineraryItem> buildItinerary(@RequestBody Map<String, Object> body) {
        LocalDate now = LocalDate.now();
        int daysToSat = (6 - now.getDayOfWeek().getValue() + 7) % 7;
        if (daysToSat == 0) daysToSat = 7;
        String weekend = (String) body.getOrDefault("weekend", "THIS");
        if ("NEXT".equals(weekend)) daysToSat += 7;
        LocalDate sat = now.plusDays(daysToSat);
        LocalDate sun = sat.plusDays(1);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        return List.of(
            new ItineraryItem("exp-001", "Morning Yoga & Meditation", "🧘", "#059669",
                sat.format(fmt), "07:00", "Club Lawn", "Phase 1", null, true, "Bring your mat"),
            new ItineraryItem("exp-003", "Community Cricket T20", "🏏", "#0891B2",
                sat.format(fmt), "10:00", "Sports Complex", "Block D", 150.0, false, "Gear provided"),
            new ItineraryItem("exp-004", "Bollywood Movie Night", "🎬", "#7C3AED",
                sat.format(fmt), "19:30", "Community Hall", "Main Block", 200.0, false, "Popcorn included 🍿"),
            new ItineraryItem("exp-002", "Photography Walk: Golden Hour", "📸", "#D97706",
                sun.format(fmt), "06:30", "Garden Walk", "East Wing", null, true, "Golden hour shoot"),
            new ItineraryItem("exp-005", "Cooking Workshop: North Indian", "👨‍🍳", "#DB2777",
                sun.format(fmt), "11:00", "Community Kitchen", "Block B", 350.0, false, "Ingredients included")
        );
    }

    @GetMapping("/buddy/matches")
    public List<InterestMatch> getInterestMatches() {
        return MOCK_MATCHES;
    }

    @PostMapping("/buddy/chat")
    public BuddyMessage buddyChat(@RequestBody Map<String, Object> body) {
        String text = ((String) body.getOrDefault("message", "")).toLowerCase();
        String response;
        if (text.contains("weekend") || text.contains("plan")) {
            response = "Great idea! I can plan a personalised weekend for you based on your interests. Use the Itinerary Planner above to generate your schedule! 🗓️";
        } else if (text.contains("free") || text.contains("budget")) {
            response = "Free this week: Morning Yoga (Club Lawn), Photography Walk (East Wing), and Nature Walk. Want me to add these to your plan? 🎁";
        } else if (text.contains("family") || text.contains("kid")) {
            response = "Family picks this weekend: Kids' Art Fiesta, Family Movie Night, and the Nature Walk suitable for all ages. Which sounds fun? 👨‍👩‍👧";
        } else if (text.contains("sport") || text.contains("cricket") || text.contains("fitness")) {
            response = "Cricket T20 is on Saturday at 10am with spots available! Also Yoga daily at 7am. Want me to book? 🏏";
        } else {
            response = "I'd love to help! I can suggest experiences, plan your weekend, or find residents with similar interests. What would you like? 😊";
        }
        return new BuddyMessage("msg-" + System.currentTimeMillis(), "buddy", response,
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
