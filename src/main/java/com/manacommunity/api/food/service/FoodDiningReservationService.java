package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.food.entity.*;
import com.manacommunity.api.food.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodDiningReservationService {

    private final FoodDiningReservationRepository reservationRepo;
    private final FoodDiningWaitlistRepository waitlistRepo;
    private final FoodDiningEventRepository eventRepo;
    private final FoodDiningFeedbackRepository feedbackRepo;

    @Transactional
    public Map<String, Object> create(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        String confirmationCode = "RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FoodDiningReservation reservation = FoodDiningReservation.builder()
                .restaurantId(request.get("restaurantId") != null ?
                        Long.valueOf(request.get("restaurantId").toString()) : null)
                .user(user)
                .reservationType(request.get("reservationType") != null ?
                        FoodDiningReservation.ReservationType.valueOf((String) request.get("reservationType")) :
                        FoodDiningReservation.ReservationType.RESTAURANT)
                .date(LocalDate.parse((String) request.get("date")))
                .time(LocalTime.parse((String) request.get("time")))
                .partySize(Integer.valueOf(request.get("partySize").toString()))
                .status(FoodDiningReservation.ReservationStatus.PENDING)
                .tableId(request.get("tableId") != null ?
                        Long.valueOf(request.get("tableId").toString()) : null)
                .specialRequests((String) request.get("specialRequests"))
                .occasion((String) request.get("occasion"))
                .preOrderId(request.get("preOrderId") != null ?
                        Long.valueOf(request.get("preOrderId").toString()) : null)
                .confirmationCode(confirmationCode)
                .community(community)
                .build();

        return toResponse(reservationRepo.save(reservation));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyReservations(Long communityId, Long userId, Pageable pageable) {
        return reservationRepo.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long communityId, Long id) {
        FoodDiningReservation reservation = reservationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningReservation", id));
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRestaurantReservations(Long communityId, Long restaurantId, LocalDate date, String status) {
        FoodDiningReservation.ReservationStatus reservationStatus =
                FoodDiningReservation.ReservationStatus.valueOf(status);
        return reservationRepo.findByRestaurantIdAndDateAndStatus(restaurantId, date, reservationStatus)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateStatus(Long communityId, Long id, String status) {
        FoodDiningReservation reservation = reservationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningReservation", id));
        reservation.setStatus(FoodDiningReservation.ReservationStatus.valueOf(status));
        return toResponse(reservationRepo.save(reservation));
    }

    @Transactional
    public Map<String, Object> checkIn(Long communityId, Long id) {
        FoodDiningReservation reservation = reservationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningReservation", id));
        reservation.setCheckedInAt(LocalDateTime.now());
        reservation.setStatus(FoodDiningReservation.ReservationStatus.SEATED);
        return toResponse(reservationRepo.save(reservation));
    }

    @Transactional
    public Map<String, Object> cancel(Long communityId, Long id) {
        FoodDiningReservation reservation = reservationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningReservation", id));
        reservation.setStatus(FoodDiningReservation.ReservationStatus.CANCELLED);
        return toResponse(reservationRepo.save(reservation));
    }

    @Transactional
    public Map<String, Object> joinWaitlist(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        Long restaurantId = Long.valueOf(request.get("restaurantId").toString());
        Integer partySize = Integer.valueOf(request.get("partySize").toString());
        FoodDiningWaitlist entry = FoodDiningWaitlist.builder()
                .restaurantId(restaurantId)
                .user(user)
                .partySize(partySize)
                .status(FoodDiningWaitlist.WaitlistStatus.WAITING)
                .joinedAt(LocalDateTime.now())
                .community(community)
                .build();
        entry = waitlistRepo.save(entry);

        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.getId());
        map.put("restaurantId", entry.getRestaurantId());
        map.put("userId", entry.getUser().getId());
        map.put("partySize", entry.getPartySize());
        map.put("estimatedWait", entry.getEstimatedWait());
        map.put("status", entry.getStatus().name());
        map.put("joinedAt", entry.getJoinedAt());
        map.put("createdAt", entry.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWaitlist(Long communityId, Long restaurantId) {
        return waitlistRepo.findByRestaurantIdAndStatus(restaurantId, FoodDiningWaitlist.WaitlistStatus.WAITING)
                .stream().map(w -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", w.getId());
                    map.put("restaurantId", w.getRestaurantId());
                    map.put("userId", w.getUser().getId());
                    map.put("partySize", w.getPartySize());
                    map.put("estimatedWait", w.getEstimatedWait());
                    map.put("status", w.getStatus().name());
                    map.put("joinedAt", w.getJoinedAt());
                    map.put("notifiedAt", w.getNotifiedAt());
                    map.put("seatedAt", w.getSeatedAt());
                    map.put("createdAt", w.getCreatedAt());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getDiningEvents(Long communityId, Pageable pageable) {
        return eventRepo.findByCommunityIdAndStatus(communityId,
                FoodDiningEvent.DiningEventStatus.PUBLISHED, pageable)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("name", e.getName());
                    map.put("description", e.getDescription());
                    map.put("venue", e.getVenue());
                    map.put("eventType", e.getEventType() != null ? e.getEventType().name() : null);
                    map.put("date", e.getDate());
                    map.put("time", e.getTime());
                    map.put("capacity", e.getCapacity());
                    map.put("booked", e.getBooked());
                    map.put("price", e.getPrice());
                    map.put("menu", e.getMenu());
                    map.put("imageUrl", e.getImageUrl());
                    map.put("organizerId", e.getOrganizerId());
                    map.put("status", e.getStatus() != null ? e.getStatus().name() : null);
                    map.put("communityId", e.getCommunity() != null ? e.getCommunity().getId() : null);
                    map.put("createdAt", e.getCreatedAt());
                    map.put("updatedAt", e.getUpdatedAt());
                    return map;
                });
    }

    @Transactional
    public Map<String, Object> submitFeedback(Long communityId, Long reservationId, Map<String, Object> request, AppUser user) {
        Community community = new Community();
        community.setId(communityId);

        FoodDiningFeedback feedback = FoodDiningFeedback.builder()
                .reservationId(reservationId)
                .user(user)
                .overallRating(request.get("overallRating") != null ?
                        Integer.valueOf(request.get("overallRating").toString()) : null)
                .foodRating(request.get("foodRating") != null ?
                        Integer.valueOf(request.get("foodRating").toString()) : null)
                .serviceRating(request.get("serviceRating") != null ?
                        Integer.valueOf(request.get("serviceRating").toString()) : null)
                .ambianceRating(request.get("ambianceRating") != null ?
                        Integer.valueOf(request.get("ambianceRating").toString()) : null)
                .comments((String) request.get("comments"))
                .community(community)
                .build();
        feedback = feedbackRepo.save(feedback);

        Map<String, Object> map = new HashMap<>();
        map.put("id", feedback.getId());
        map.put("reservationId", feedback.getReservationId());
        map.put("userId", feedback.getUser().getId());
        map.put("overallRating", feedback.getOverallRating());
        map.put("foodRating", feedback.getFoodRating());
        map.put("serviceRating", feedback.getServiceRating());
        map.put("ambianceRating", feedback.getAmbianceRating());
        map.put("comments", feedback.getComments());
        map.put("createdAt", feedback.getCreatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodDiningReservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("restaurantId", r.getRestaurantId());
        map.put("userId", r.getUser() != null ? r.getUser().getId() : null);
        map.put("reservationType", r.getReservationType() != null ? r.getReservationType().name() : null);
        map.put("date", r.getDate());
        map.put("time", r.getTime());
        map.put("partySize", r.getPartySize());
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("tableId", r.getTableId());
        map.put("specialRequests", r.getSpecialRequests());
        map.put("occasion", r.getOccasion());
        map.put("preOrderId", r.getPreOrderId());
        map.put("confirmationCode", r.getConfirmationCode());
        map.put("checkedInAt", r.getCheckedInAt());
        map.put("communityId", r.getCommunity() != null ? r.getCommunity().getId() : null);
        map.put("createdAt", r.getCreatedAt());
        map.put("updatedAt", r.getUpdatedAt());
        return map;
    }
}
