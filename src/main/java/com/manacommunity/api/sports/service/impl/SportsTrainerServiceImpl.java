package com.manacommunity.api.sports.service.impl;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.SportsTrainer;
import com.manacommunity.api.sports.model.TrainerSlot;
import com.manacommunity.api.sports.model.TrainerSlotBooking;
import com.manacommunity.api.sports.repository.SportsTrainerRepository;
import com.manacommunity.api.sports.repository.TrainerSlotBookingRepository;
import com.manacommunity.api.sports.repository.TrainerSlotRepository;
import com.manacommunity.api.sports.service.SportsTrainerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTrainerServiceImpl implements SportsTrainerService {

    private final SportsTrainerRepository trainerRepo;
    private final TrainerSlotRepository slotRepo;
    private final TrainerSlotBookingRepository bookingRepo;
    private final CommunityRepository communityRepo;

    // ── Apply ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SportsTrainerResponse apply(Long communityId, Long userId, SportsTrainerRequest req) {
        if (trainerRepo.existsByCommunity_IdAndUserIdAndStatusNot(communityId, userId, "REJECTED")) {
            throw new InvalidInputException("An active application already exists for this user");
        }
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));

        SportsTrainer trainer = SportsTrainer.builder()
                .community(community)
                .userId(userId)
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .sport(req.getSport())
                .certBody(req.getCertBody())
                .certNumber(req.getCertNumber())
                .certYear(req.getCertYear())
                .ageGroupsCoached(req.getAgeGroupsCoached())
                .bio(req.getBio())
                .experienceYears(req.getExperienceYears())
                .profilePicUrl(req.getProfilePicUrl())
                .status("APPLIED")
                .build();

        return toResponse(trainerRepo.save(trainer));
    }

    // ── Listing ──────────────────────────────────────────────────────────────

    @Override
    public List<SportsTrainerResponse> listApproved(Long communityId) {
        return trainerRepo.findByCommunity_IdAndStatusOrderByCreatedAtDesc(communityId, "APPROVED")
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<SportsTrainerResponse> listAll(Long communityId) {
        return trainerRepo.findByCommunity_IdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Status update ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SportsTrainerResponse updateStatus(Long trainerId, String status, Long adminUserId, String rejectionReason) {
        SportsTrainer trainer = trainerRepo.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerId));

        trainer.setStatus(status);
        if ("APPROVED".equals(status)) {
            trainer.setApprovedBy(adminUserId);
            trainer.setApprovedAt(LocalDateTime.now());
        } else if ("REJECTED".equals(status)) {
            trainer.setRejectionReason(rejectionReason);
        }
        return toResponse(trainerRepo.save(trainer));
    }

    // ── Slots ────────────────────────────────────────────────────────────────

    @Override
    public List<TrainerSlotResponse> listSlots(Long trainerId) {
        return slotRepo.findByTrainer_IdAndStatusOrderBySlotDateAscStartTimeAsc(trainerId, "OPEN")
                .stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrainerSlotResponse createSlot(Long communityId, TrainerSlotRequest req) {
        SportsTrainer trainer = trainerRepo.findById(req.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + req.getTrainerId()));

        TrainerSlot slot = TrainerSlot.builder()
                .trainer(trainer)
                .sport(req.getSport())
                .slotDate(req.getSlotDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .durationMinutes(req.getDurationMinutes())
                .slotType(req.getSlotType() != null ? req.getSlotType() : "INDIVIDUAL")
                .maxCapacity(req.getMaxCapacity() != null ? req.getMaxCapacity() : 1)
                .venueId(req.getVenueId())
                .venueName(req.getVenueName())
                .price(req.getPrice())
                .notes(req.getNotes())
                .communityId(communityId)
                .build();

        return toSlotResponse(slotRepo.save(slot));
    }

    // ── Bookings ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TrainerSlotBookingResponse bookSlot(Long slotId, Long userId, TrainerSlotBookingRequest req) {
        TrainerSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        if (!"OPEN".equals(slot.getStatus())) {
            throw new InvalidInputException("Slot is not available for booking");
        }
        if (slot.getBookedCount() >= slot.getMaxCapacity()) {
            throw new InvalidInputException("Slot is fully booked");
        }
        if (bookingRepo.existsBySlot_IdAndUserIdAndStatus(slotId, userId, "CONFIRMED")) {
            throw new InvalidInputException("You have already booked this slot");
        }

        TrainerSlotBooking booking = TrainerSlotBooking.builder()
                .slot(slot)
                .trainerId(slot.getTrainer().getId())
                .userId(userId)
                .userName(req.getUserName())
                .tower(req.getTower())
                .flatNumber(req.getFlatNumber())
                .amountPaid(slot.getPrice())
                .communityId(slot.getCommunityId())
                .build();

        bookingRepo.save(booking);

        // Increment booked count; mark slot full if at capacity
        slot.setBookedCount(slot.getBookedCount() + 1);
        if (slot.getBookedCount() >= slot.getMaxCapacity()) {
            slot.setStatus("BOOKED");
        }
        slotRepo.save(slot);

        return toBookingResponse(booking);
    }

    @Override
    @Transactional
    public TrainerSlotBookingResponse cancelBooking(Long bookingId, Long userId) {
        TrainerSlotBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getUserId().equals(userId)) {
            throw new InvalidInputException("Not authorized to cancel this booking");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new InvalidInputException("Booking already cancelled");
        }

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepo.save(booking);

        // Reopen slot if it was marked BOOKED
        TrainerSlot slot = booking.getSlot();
        if ("BOOKED".equals(slot.getStatus())) {
            slot.setStatus("OPEN");
        }
        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        slotRepo.save(slot);

        return toBookingResponse(booking);
    }

    @Override
    public List<TrainerSlotBookingResponse> myBookings(Long userId, Long communityId) {
        return bookingRepo.findByUserIdAndCommunityIdOrderByBookedAtDesc(userId, communityId)
                .stream().map(this::toBookingResponse).collect(Collectors.toList());
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private SportsTrainerResponse toResponse(SportsTrainer t) {
        return SportsTrainerResponse.builder()
                .id(t.getId())
                .communityId(t.getCommunity().getId())
                .userId(t.getUserId())
                .fullName(t.getFullName())
                .email(t.getEmail())
                .phone(t.getPhone())
                .sport(t.getSport())
                .certBody(t.getCertBody())
                .certNumber(t.getCertNumber())
                .certYear(t.getCertYear())
                .ageGroupsCoached(t.getAgeGroupsCoached())
                .bio(t.getBio())
                .experienceYears(t.getExperienceYears())
                .profilePicUrl(t.getProfilePicUrl())
                .status(t.getStatus())
                .approvedAt(t.getApprovedAt())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private TrainerSlotResponse toSlotResponse(TrainerSlot s) {
        return TrainerSlotResponse.builder()
                .id(s.getId())
                .trainerId(s.getTrainer().getId())
                .trainerName(s.getTrainer().getFullName())
                .sport(s.getSport())
                .slotDate(s.getSlotDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .durationMinutes(s.getDurationMinutes())
                .slotType(s.getSlotType())
                .maxCapacity(s.getMaxCapacity())
                .bookedCount(s.getBookedCount())
                .venueId(s.getVenueId())
                .venueName(s.getVenueName())
                .price(s.getPrice())
                .status(s.getStatus())
                .notes(s.getNotes())
                .build();
    }

    private TrainerSlotBookingResponse toBookingResponse(TrainerSlotBooking b) {
        return TrainerSlotBookingResponse.builder()
                .id(b.getId())
                .slotId(b.getSlot().getId())
                .trainerId(b.getTrainerId())
                .userId(b.getUserId())
                .userName(b.getUserName())
                .tower(b.getTower())
                .flatNumber(b.getFlatNumber())
                .status(b.getStatus())
                .amountPaid(b.getAmountPaid())
                .bookedAt(b.getBookedAt())
                .slot(toSlotResponse(b.getSlot()))
                .build();
    }
}
