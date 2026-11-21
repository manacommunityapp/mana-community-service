package com.manacommunity.api.academy.session.controller;

import com.manacommunity.api.academy.session.entity.AcademySession;
import com.manacommunity.api.academy.session.entity.AcademySessionEnrollment;
import com.manacommunity.api.academy.session.service.AcademySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/academy/sessions")
@RequiredArgsConstructor
public class AcademySessionController {

    private final AcademySessionService service;

    @GetMapping
    public ResponseEntity<Page<AcademySession>> list(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getUpcomingSessions(communityId, category,
                PageRequest.of(page, size, Sort.by("startsAt").ascending())));
    }

    @GetMapping("/my-teaching")
    public ResponseEntity<List<AcademySession>> myTeaching(@RequestParam Long instructorId) {
        return ResponseEntity.ok(service.getMyTeaching(instructorId));
    }

    @GetMapping("/enrollments/my")
    public ResponseEntity<List<AcademySessionEnrollment>> myEnrollments(@RequestParam Long studentId) {
        return ResponseEntity.ok(service.getMyEnrollments(studentId));
    }

    @PostMapping
    public ResponseEntity<AcademySession> create(@RequestBody Map<String, Object> body) {
        AcademySession session = AcademySession.builder()
                .communityId(Long.valueOf(body.get("communityId").toString()))
                .instructorId(Long.valueOf(body.get("instructorId").toString()))
                .title((String) body.get("title"))
                .description((String) body.get("description"))
                .category((String) body.get("category"))
                .sessionType((String) body.getOrDefault("sessionType", "WORKSHOP"))
                .format((String) body.getOrDefault("format", "IN_PERSON"))
                .location((String) body.getOrDefault("location", null))
                .onlineLink((String) body.getOrDefault("onlineLink", null))
                .startsAt(OffsetDateTime.parse(body.get("startsAt").toString()))
                .endsAt(OffsetDateTime.parse(body.get("endsAt").toString()))
                .maxSeats(body.containsKey("maxSeats") ? Integer.parseInt(body.get("maxSeats").toString()) : null)
                .price(body.containsKey("price") ? new BigDecimal(body.get("price").toString()) : BigDecimal.ZERO)
                .coverImage((String) body.getOrDefault("coverImage", null))
                .build();
        return ResponseEntity.ok(service.createSession(session));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<AcademySession> publish(@PathVariable Long id, @RequestParam Long instructorId) {
        return ResponseEntity.ok(service.publishSession(id, instructorId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AcademySession> cancel(@PathVariable Long id, @RequestParam Long instructorId) {
        return ResponseEntity.ok(service.cancelSession(id, instructorId));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<AcademySessionEnrollment> enroll(@PathVariable Long id, @RequestParam Long studentId) {
        return ResponseEntity.ok(service.enroll(id, studentId));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Map<String, Boolean>> cancelEnrollment(@PathVariable Long id,
            @RequestParam Long studentId) {
        service.cancelEnrollment(id, studentId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/enrollments/{id}/feedback")
    public ResponseEntity<Map<String, Boolean>> feedback(@PathVariable Long id,
            @RequestParam Long studentId,
            @RequestBody Map<String, Object> body) {
        int rating = Integer.parseInt(body.get("rating").toString());
        String text = (String) body.getOrDefault("text", null);
        service.submitFeedback(id, studentId, rating, text);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
