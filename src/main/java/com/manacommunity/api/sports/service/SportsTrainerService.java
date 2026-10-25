package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.*;

import java.util.List;

public interface SportsTrainerService {

    /** Resident submits a trainer application */
    SportsTrainerResponse apply(Long communityId, Long userId, SportsTrainerRequest request);

    /** List all approved trainers in a community */
    List<SportsTrainerResponse> listApproved(Long communityId);

    /** Admin: list all applications (all statuses) */
    List<SportsTrainerResponse> listAll(Long communityId);

    /** Admin: approve or reject */
    SportsTrainerResponse updateStatus(Long trainerId, String status, Long adminUserId, String rejectionReason);

    /** List open slots for a trainer */
    List<TrainerSlotResponse> listSlots(Long trainerId);

    /** Trainer/admin creates a slot */
    TrainerSlotResponse createSlot(Long communityId, TrainerSlotRequest request);

    /** Resident books a slot */
    TrainerSlotBookingResponse bookSlot(Long slotId, Long userId, TrainerSlotBookingRequest request);

    /** Resident cancels their booking */
    TrainerSlotBookingResponse cancelBooking(Long bookingId, Long userId);

    /** Resident's own bookings */
    List<TrainerSlotBookingResponse> myBookings(Long userId, Long communityId);
}
