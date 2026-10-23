package com.manacommunity.api.controller;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/movies")
@CrossOrigin(origins = "*")
public class MovieController {

    // ── DTOs ─────────────────────────────────────────────────────────────────

    record Movie(String id, String title, String language, List<String> genre,
                 String rating, int durationMins, String posterEmoji, String backdropColor,
                 String cast, String director, String releaseDate, String certificate,
                 String status) {}

    record Showtime(String id, String movieId, String theater, String theaterScreen,
                    String date, String time, int priceClassic, int pricePremium, int priceRecliner,
                    int availableSeats, int totalSeats, String format) {}

    record SeatInfo(int number, String status) {}
    record SeatRow(String row, String seatType, List<SeatInfo> seats) {}

    record BookRequest(String showtimeId, String movieId, List<String> seats,
                       String seatType, double amount) {}

    record BookResult(String bookingId, String upiIntentUrl, String phonePeUrl,
                      String transactionRef, double amount, List<String> seats, String qrData) {}

    // ── Mock Data ─────────────────────────────────────────────────────────────

    private static final List<Movie> NOW_SHOWING = List.of(
        new Movie("m1", "Pushpa 2: The Rule", "Telugu", List.of("Action", "Drama"), "8.5", 179, "🔥", "#7C2D12", "Allu Arjun, Rashmika Mandanna", "Sukumar", "2024-12-05", "UA", "NOW_SHOWING"),
        new Movie("m2", "Devara", "Telugu", List.of("Action", "Thriller"), "7.8", 166, "⚔️", "#1E3A5F", "Jr. NTR, Janhvi Kapoor", "Koratala Siva", "2024-09-27", "UA", "NOW_SHOWING"),
        new Movie("m3", "Singham Again", "Hindi", List.of("Action", "Comedy"), "7.2", 162, "👮", "#1E293B", "Ajay Devgn, Deepika Padukone", "Rohit Shetty", "2024-11-01", "UA", "NOW_SHOWING"),
        new Movie("m4", "Venom: The Last Dance", "English", List.of("Action", "Sci-Fi"), "6.9", 109, "🕷️", "#1A1A2E", "Tom Hardy, Juno Temple", "Kelly Marcel", "2024-10-25", "UA", "NOW_SHOWING"),
        new Movie("m5", "Kanguva", "Tamil", List.of("Action", "Fantasy"), "5.8", 164, "⚡", "#4A044E", "Suriya, Disha Patani", "Siva", "2024-11-14", "UA", "NOW_SHOWING"),
        new Movie("m6", "Lucky Baskhar", "Telugu", List.of("Drama", "Thriller"), "8.2", 154, "🎰", "#14532D", "Dulquer Salmaan, Meenakshi Chaudhary", "Venky Atluri", "2024-10-31", "UA", "NOW_SHOWING")
    );

    private static final List<Movie> COMING_SOON = List.of(
        new Movie("m7", "Kalki 2898 AD – Part 2", "Telugu", List.of("Sci-Fi", "Action"), "TBD", 180, "🚀", "#292524", "Prabhas, Deepika Padukone", "Nag Ashwin", "2025-01-12", "UA", "COMING_SOON"),
        new Movie("m8", "Deva", "Hindi", List.of("Action", "Thriller"), "TBD", 150, "🎯", "#1C1917", "Shahid Kapoor, Pooja Hegde", "Rosshan Andrrews", "2025-01-31", "UA", "COMING_SOON"),
        new Movie("m9", "Mufasa: The Lion King", "English", List.of("Animation", "Family"), "TBD", 118, "🦁", "#7C2D12", "Aaron Pierre, Kelvin Harrison Jr.", "Barry Jenkins", "2024-12-20", "U", "COMING_SOON")
    );

    private static final List<Map<String, Object>> THEATERS = List.of(
        Map.of("name", "Mana Community Screen", "screen", "Screen 1", "format", "2D"),
        Map.of("name", "PVR Cinemas", "screen", "PVR Gold", "format", "3D"),
        Map.of("name", "INOX Multiplex", "screen", "INOX Screen 3", "format", "IMAX")
    );

    private static final String[] TIMES = {"10:00 AM", "1:30 PM", "4:45 PM", "8:00 PM", "10:30 PM"};

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping
    public Map<String, Object> getMovies() {
        return Map.of("now_showing", NOW_SHOWING, "coming_soon", COMING_SOON);
    }

    @GetMapping("/{movieId}/showtimes")
    public List<Showtime> getShowtimes(@PathVariable String movieId) {
        List<Showtime> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        Random rng = new Random(movieId.hashCode());

        for (int d = 0; d < 3; d++) {
            String date = today.plusDays(d).format(fmt);
            for (int ti = 0; ti < THEATERS.size(); ti++) {
                Map<String, Object> theater = THEATERS.get(ti);
                int startTime = ti;
                int endTime = Math.min(ti + 3, TIMES.length);
                for (int idx = startTime; idx < endTime; idx++) {
                    String format = (String) theater.get("format");
                    result.add(new Showtime(
                        "st-" + movieId + "-" + date + "-" + ti + "-" + idx,
                        movieId, (String) theater.get("name"), (String) theater.get("screen"),
                        date, TIMES[idx], 180, 280, 450,
                        20 + Math.abs(rng.nextInt() % 80), 120, format
                    ));
                }
            }
        }
        return result;
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    public List<SeatRow> getSeatLayout(@PathVariable String showtimeId) {
        List<SeatRow> rows = new ArrayList<>();
        Random rng = new Random(showtimeId.hashCode());

        char[] classicRows = {'A', 'B', 'C', 'D', 'E'};
        for (char row : classicRows) {
            List<SeatInfo> seats = new ArrayList<>();
            for (int s = 1; s <= 12; s++) {
                seats.add(new SeatInfo(s, rng.nextDouble() < 0.35 ? "OCCUPIED" : "AVAILABLE"));
            }
            rows.add(new SeatRow(String.valueOf(row), "CLASSIC", seats));
        }

        char[] premiumRows = {'F', 'G', 'H', 'I', 'J'};
        for (char row : premiumRows) {
            List<SeatInfo> seats = new ArrayList<>();
            for (int s = 1; s <= 10; s++) {
                seats.add(new SeatInfo(s, rng.nextDouble() < 0.35 ? "OCCUPIED" : "AVAILABLE"));
            }
            rows.add(new SeatRow(String.valueOf(row), "PREMIUM", seats));
        }
        return rows;
    }

    @PostMapping("/book")
    public BookResult book(@RequestBody BookRequest req) {
        String bookingId = "BMS-MANA-" + System.currentTimeMillis();
        String tr = "MANA-MOVIE-" + System.currentTimeMillis();
        String seatStr = String.join(",", req.seats());
        return new BookResult(
            bookingId,
            "upi://pay?pa=movies@setu&pn=Mana+Movies&am=" + String.format("%.2f", req.amount())
                + "&cu=INR&tr=" + tr + "&tn=Movie+Ticket+" + bookingId,
            "phonepe://pay?pa=movies@setu&pn=Mana+Movies&am=" + String.format("%.2f", req.amount())
                + "&cu=INR&tr=" + tr,
            tr, req.amount(), req.seats(),
            bookingId + "|" + seatStr + "|" + req.amount()
        );
    }
}
