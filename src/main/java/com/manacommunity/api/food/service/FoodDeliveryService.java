package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodDeliveryAssignment;
import com.manacommunity.api.food.entity.FoodDeliveryPartner;
import com.manacommunity.api.food.entity.FoodDeliveryZone;
import com.manacommunity.api.food.entity.FoodOrder;
import com.manacommunity.api.food.repository.FoodDeliveryAssignmentRepository;
import com.manacommunity.api.food.repository.FoodDeliveryPartnerRepository;
import com.manacommunity.api.food.repository.FoodDeliveryZoneRepository;
import com.manacommunity.api.food.repository.FoodOrderRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodDeliveryService {

    private final FoodDeliveryPartnerRepository partnerRepo;
    private final FoodDeliveryAssignmentRepository assignmentRepo;
    private final FoodDeliveryZoneRepository zoneRepo;
    private final FoodOrderRepository orderRepo;

    @Transactional
    public Map<String, Object> register(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        FoodDeliveryPartner partner = FoodDeliveryPartner.builder()
                .user(user)
                .vehicleType(FoodDeliveryPartner.VehicleType.valueOf((String) request.get("vehicleType")))
                .vehicleNumber((String) request.get("vehicleNumber"))
                .licenseNumber((String) request.get("licenseNumber"))
                .status(FoodDeliveryPartner.PartnerStatus.OFFLINE)
                .activeZone((String) request.get("activeZone"))
                .community(community)
                .build();

        FoodDeliveryPartner saved = partnerRepo.save(partner);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailable(Long communityId) {
        List<FoodDeliveryPartner> partners = partnerRepo.findByCommunityIdAndStatus(
                communityId, FoodDeliveryPartner.PartnerStatus.AVAILABLE.name());
        return partners.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> assignDelivery(Long communityId, Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        Long partnerId = Long.valueOf(request.get("partnerId").toString());

        FoodOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodOrder", orderId));

        FoodDeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodDeliveryPartner", partnerId));

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        FoodDeliveryAssignment assignment = FoodDeliveryAssignment.builder()
                .order(order)
                .partner(partner)
                .status(FoodDeliveryAssignment.AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .otpCode(otpCode)
                .community(order.getCommunity())
                .build();

        FoodDeliveryAssignment saved = assignmentRepo.save(assignment);

        partner.setStatus(FoodDeliveryPartner.PartnerStatus.ON_DELIVERY);
        partnerRepo.save(partner);

        return toAssignmentResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long communityId, Long assignmentId, String status, BigDecimal latitude, BigDecimal longitude) {
        FoodDeliveryAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodDeliveryAssignment", assignmentId));

        FoodDeliveryAssignment.AssignmentStatus newStatus = FoodDeliveryAssignment.AssignmentStatus.valueOf(status);
        assignment.setStatus(newStatus);

        switch (newStatus) {
            case ACCEPTED:
                assignment.setAcceptedAt(LocalDateTime.now());
                break;
            case PICKED_UP:
                assignment.setPickedUpAt(LocalDateTime.now());
                break;
            case DELIVERED:
                assignment.setDeliveredAt(LocalDateTime.now());
                FoodDeliveryPartner partner = assignment.getPartner();
                partner.setStatus(FoodDeliveryPartner.PartnerStatus.AVAILABLE);
                partner.setTotalDeliveries(partner.getTotalDeliveries() + 1);
                partnerRepo.save(partner);
                break;
            case CANCELLED:
                FoodDeliveryPartner cancelledPartner = assignment.getPartner();
                cancelledPartner.setStatus(FoodDeliveryPartner.PartnerStatus.AVAILABLE);
                partnerRepo.save(cancelledPartner);
                break;
            default:
                break;
        }

        if (latitude != null && longitude != null) {
            FoodDeliveryPartner p = assignment.getPartner();
            p.setCurrentLatitude(latitude);
            p.setCurrentLongitude(longitude);
            partnerRepo.save(p);
        }

        FoodDeliveryAssignment saved = assignmentRepo.save(assignment);
        return toAssignmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean verifyOtp(Long communityId, Long assignmentId, String otp) {
        FoodDeliveryAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodDeliveryAssignment", assignmentId));
        return otp != null && otp.equals(assignment.getOtpCode());
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyDeliveries(Long communityId, Long userId, String status, Pageable pageable) {
        FoodDeliveryPartner me = partnerRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodDeliveryPartner", "userId", userId.toString()));
        Long partnerId = me.getId();
        if (status != null && !status.isBlank()) {
            List<FoodDeliveryAssignment> assignments = assignmentRepo.findByPartnerIdAndStatus(partnerId, status);
            List<Map<String, Object>> result = assignments.stream()
                    .map(this::toAssignmentResponse).collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(result, pageable, result.size());
        }
        Page<FoodDeliveryAssignment> assignments = assignmentRepo.findAll(pageable);
        return assignments.map(this::toAssignmentResponse);
    }

    @Transactional
    public Map<String, Object> updateLocation(Long communityId, Map<String, Object> request) {
        Long partnerId = Long.valueOf(request.get("partnerId").toString());
        BigDecimal lat = request.containsKey("latitude") ? new BigDecimal(request.get("latitude").toString()) : null;
        BigDecimal lng = request.containsKey("longitude") ? new BigDecimal(request.get("longitude").toString()) : null;

        FoodDeliveryPartner partner = partnerRepo.findByIdAndCommunityId(partnerId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodDeliveryPartner", partnerId));

        partner.setCurrentLatitude(lat);
        partner.setCurrentLongitude(lng);

        FoodDeliveryPartner saved = partnerRepo.save(partner);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getZones(Long communityId) {
        List<FoodDeliveryZone> zones = zoneRepo.findByCommunityIdAndActive(communityId, true);
        return zones.stream().map(z -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", z.getId());
            map.put("zoneName", z.getZoneName());
            map.put("zoneType", z.getZoneType().name());
            map.put("polygon", z.getPolygon());
            map.put("baseDeliveryFee", z.getBaseDeliveryFee());
            map.put("perKmFee", z.getPerKmFee());
            map.put("surgeMultiplier", z.getSurgeMultiplier());
            map.put("active", z.getActive());
            map.put("communityId", z.getCommunity().getId());
            map.put("createdAt", z.getCreatedAt());
            map.put("updatedAt", z.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toResponse(FoodDeliveryPartner partner) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", partner.getId());
        map.put("userId", partner.getUser().getId());
        map.put("vehicleType", partner.getVehicleType().name());
        map.put("vehicleNumber", partner.getVehicleNumber());
        map.put("licenseNumber", partner.getLicenseNumber());
        map.put("status", partner.getStatus().name());
        map.put("currentLatitude", partner.getCurrentLatitude());
        map.put("currentLongitude", partner.getCurrentLongitude());
        map.put("rating", partner.getRating());
        map.put("totalDeliveries", partner.getTotalDeliveries());
        map.put("activeZone", partner.getActiveZone());
        map.put("communityId", partner.getCommunity().getId());
        map.put("createdAt", partner.getCreatedAt());
        map.put("updatedAt", partner.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toAssignmentResponse(FoodDeliveryAssignment assignment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", assignment.getId());
        map.put("orderId", assignment.getOrder().getId());
        map.put("partnerId", assignment.getPartner().getId());
        map.put("status", assignment.getStatus().name());
        map.put("assignedAt", assignment.getAssignedAt());
        map.put("acceptedAt", assignment.getAcceptedAt());
        map.put("pickedUpAt", assignment.getPickedUpAt());
        map.put("deliveredAt", assignment.getDeliveredAt());
        map.put("distanceKm", assignment.getDistanceKm());
        map.put("deliveryFee", assignment.getDeliveryFee());
        map.put("tip", assignment.getTip());
        map.put("otpCode", assignment.getOtpCode());
        map.put("communityId", assignment.getCommunity().getId());
        map.put("createdAt", assignment.getCreatedAt());
        map.put("updatedAt", assignment.getUpdatedAt());
        return map;
    }
}
