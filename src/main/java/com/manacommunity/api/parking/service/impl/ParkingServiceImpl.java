package com.manacommunity.api.parking.service.impl;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.dto.ParkingSpotDto;
import com.manacommunity.api.parking.dto.ReserveSpotRequest;
import com.manacommunity.api.parking.dto.VisitorPassDto;
import com.manacommunity.api.parking.dto.VisitorPassRequest;
import com.manacommunity.api.parking.entity.ParkingSpot;
import com.manacommunity.api.parking.entity.ParkingVisitorPass;
import com.manacommunity.api.parking.repository.ParkingSpotRepository;
import com.manacommunity.api.parking.repository.ParkingVisitorPassRepository;
import com.manacommunity.api.parking.service.ParkingService;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParkingSpotRepository spotRepository;
    private final ParkingVisitorPassRepository visitorPassRepository;
    private final CommunityRepository communityRepository;

    @Override
    @Transactional
    public List<ParkingSpotDto> getSpots(AppUser user, String type, String status, String level) {
        Community community = resolveCommunity(user);
        seedDefaultSpotsIfEmpty(community, user);

        List<ParkingSpot> spots = spotRepository.findByCommunityId(community.getId());

        return spots.stream()
                .filter(s -> type == null || type.isBlank() || s.getSpotType().equalsIgnoreCase(type))
                .filter(s -> status == null || status.isBlank() || s.getStatus().equalsIgnoreCase(status))
                .filter(s -> level == null || level.isBlank() || s.getLevel().equalsIgnoreCase(level))
                .map(s -> toSpotDto(s, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ParkingSpotDto> getMySpots(AppUser user) {
        Community community = resolveCommunity(user);
        seedDefaultSpotsIfEmpty(community, user);

        List<ParkingSpot> userSpots = spotRepository.findByCommunityIdAndAssignedUserId(community.getId(), user.getId());
        return userSpots.stream()
                .map(s -> toSpotDto(s, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParkingSpotDto reserveSpot(AppUser user, ReserveSpotRequest request) {
        Community community = resolveCommunity(user);
        ParkingSpot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking Spot", request.getSpotId()));

        if (!spot.getCommunity().getId().equals(community.getId())) {
            throw new InvalidInputException("Selected parking spot does not belong to your community.");
        }

        if (!"AVAILABLE".equalsIgnoreCase(spot.getStatus())) {
            throw new InvalidInputException("Spot " + spot.getSpotNumber() + " is currently " + spot.getStatus() + " and cannot be reserved.");
        }

        spot.setStatus("RESERVED");
        spot.setAssignedUser(user);
        spot.setVehicleNumber(request.getVehicleNumber().trim());
        spot.setOwnerName(user.getFullName() != null ? user.getFullName() : "Resident");
        spot.setOwnerFlat(user.getFlatNo() != null ? user.getFlatNo() : (user.getBlock() != null ? user.getBlock() : "Unit"));
        if (request.getVehicleType() != null && !request.getVehicleType().isBlank()) {
            spot.setSpotType(request.getVehicleType().toUpperCase());
        }
        if (request.getNotes() != null) {
            spot.setNotes(request.getNotes().trim());
        }

        ParkingSpot saved = spotRepository.save(spot);
        log.info("Parking spot {} reserved by user {}", saved.getSpotNumber(), user.getId());
        return toSpotDto(saved, user);
    }

    @Override
    @Transactional
    public VisitorPassDto createVisitorPass(AppUser user, VisitorPassRequest request) {
        Community community = resolveCommunity(user);

        if (request.getVisitorName() == null || request.getVisitorName().isBlank()) {
            throw new InvalidInputException("Visitor name is required.");
        }
        if (request.getVehicleNumber() == null || request.getVehicleNumber().isBlank()) {
            throw new InvalidInputException("Vehicle number is required.");
        }

        ParkingSpot spot = null;
        if (request.getSpotId() != null) {
            spot = spotRepository.findById(request.getSpotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parking Spot", request.getSpotId()));
            if (!spot.getCommunity().getId().equals(community.getId())) {
                throw new InvalidInputException("Selected parking spot does not belong to your community.");
            }
        }

        LocalDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : LocalDateTime.now();
        LocalDateTime validUntil = request.getValidUntil() != null ? request.getValidUntil() : validFrom.plusHours(8);

        if (validUntil.isBefore(validFrom)) {
            throw new InvalidInputException("Valid until time must be after valid from time.");
        }

        String passCode = "VP-" + (100000 + RANDOM.nextInt(900000));

        ParkingVisitorPass pass = ParkingVisitorPass.builder()
                .community(community)
                .user(user)
                .passCode(passCode)
                .visitorName(request.getVisitorName().trim())
                .visitorPhone(request.getVisitorPhone() != null ? request.getVisitorPhone().trim() : null)
                .vehicleNumber(request.getVehicleNumber().trim().toUpperCase())
                .vehicleType(request.getVehicleType() != null ? request.getVehicleType().toUpperCase() : "CAR")
                .spot(spot)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .purpose(request.getPurpose() != null ? request.getPurpose().trim() : "Visitor Parking")
                .status("ACTIVE")
                .build();

        ParkingVisitorPass saved = visitorPassRepository.save(pass);
        log.info("Visitor pass {} issued for vehicle {} by user {}", passCode, saved.getVehicleNumber(), user.getId());

        return toVisitorPassDto(saved);
    }

    private Community resolveCommunity(AppUser user) {
        if (user.getCommunity() != null) {
            return user.getCommunity();
        }
        return communityRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No active community found for parking management."));
    }

    private void seedDefaultSpotsIfEmpty(Community community, AppUser user) {
        long count = spotRepository.countByCommunityId(community.getId());
        if (count > 0) {
            return;
        }

        log.info("Auto-seeding initial 6 parking spots for community {}", community.getName());
        List<ParkingSpot> seeds = new ArrayList<>();

        seeds.add(ParkingSpot.builder()
                .community(community)
                .spotNumber("B1-P12")
                .level("Basement 1")
                .spotType("CAR")
                .status("OCCUPIED")
                .vehicleNumber("KA-01-AB-1234")
                .ownerName("You")
                .ownerFlat(user.getFlatNo() != null ? user.getFlatNo() : "A1-302")
                .assignedUser(user)
                .build());

        seeds.add(ParkingSpot.builder()
                .community(community)
                .spotNumber("B1-P13")
                .level("Basement 1")
                .spotType("BIKE")
                .status("OCCUPIED")
                .vehicleNumber("KA-01-CD-5678")
                .ownerName("You")
                .ownerFlat(user.getFlatNo() != null ? user.getFlatNo() : "A1-302")
                .assignedUser(user)
                .build());

        seeds.add(ParkingSpot.builder()
                .community(community)
                .spotNumber("B2-P05")
                .level("Basement 2")
                .spotType("CAR")
                .status("AVAILABLE")
                .build());

        seeds.add(ParkingSpot.builder()
                .community(community)
                .spotNumber("B1-EV3")
                .level("Basement 1")
                .spotType("EV")
                .status("RESERVED")
                .ownerName("Rahul K.")
                .ownerFlat("B2-201")
                .build());

        seeds.add(ParkingSpot.builder()
                .community(community)
                .spotNumber("B2-P22")
                .level("Basement 2")
                .spotType("CAR")
                .status("AVAILABLE")
                .build());

        seeds.add(ParkingSpot.builder()
                .community(community)
                .spotNumber("B1-P08")
                .level("Basement 1")
                .spotType("BIKE")
                .status("AVAILABLE")
                .build());

        spotRepository.saveAll(seeds);
    }

    private ParkingSpotDto toSpotDto(ParkingSpot spot, AppUser currentUser) {
        boolean isOwner = spot.getAssignedUser() != null && spot.getAssignedUser().getId().equals(currentUser.getId());
        String displayOwner = isOwner ? "You" : spot.getOwnerName();

        return ParkingSpotDto.builder()
                .id(spot.getId())
                .spotNumber(spot.getSpotNumber())
                .level(spot.getLevel())
                .type(spot.getSpotType())
                .status(spot.getStatus())
                .vehicleNumber(spot.getVehicleNumber())
                .ownerName(displayOwner)
                .ownerFlat(spot.getOwnerFlat())
                .assignedUserId(spot.getAssignedUser() != null ? spot.getAssignedUser().getId() : null)
                .communityId(spot.getCommunity() != null ? spot.getCommunity().getId() : null)
                .notes(spot.getNotes())
                .build();
    }

    private VisitorPassDto toVisitorPassDto(ParkingVisitorPass pass) {
        return VisitorPassDto.builder()
                .id(pass.getId())
                .passCode(pass.getPassCode())
                .visitorName(pass.getVisitorName())
                .visitorPhone(pass.getVisitorPhone())
                .vehicleNumber(pass.getVehicleNumber())
                .vehicleType(pass.getVehicleType())
                .spotId(pass.getSpot() != null ? pass.getSpot().getId() : null)
                .spotNumber(pass.getSpot() != null ? pass.getSpot().getSpotNumber() : null)
                .validFrom(pass.getValidFrom())
                .validUntil(pass.getValidUntil())
                .purpose(pass.getPurpose())
                .status(pass.getStatus())
                .build();
    }
}
