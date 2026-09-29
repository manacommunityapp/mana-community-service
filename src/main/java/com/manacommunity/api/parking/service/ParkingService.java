package com.manacommunity.api.parking.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.parking.dto.*;
import com.manacommunity.api.parking.entity.ParkingSlot;
import com.manacommunity.api.parking.entity.ResidentVehicle;
import com.manacommunity.api.parking.repository.ParkingSlotRepository;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingSlotRepository slotRepository;
    private final ResidentVehicleRepository vehicleRepository;

    // ── Parking Slots ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ParkingSlotResponse> getSlots(Long communityId) {
        return slotRepository.findByCommunityIdOrderBySlotNumberAsc(communityId)
                .stream().map(this::toSlotResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingSlotResponse> getAvailableSlots(Long communityId) {
        return slotRepository.findByCommunityIdAndStatusOrderBySlotNumberAsc(communityId, ParkingSlot.SlotStatus.AVAILABLE)
                .stream().map(this::toSlotResponse).toList();
    }

    @Transactional
    public ParkingSlotResponse createSlot(AppUser caller, ParkingSlotRequest req) {
        Long communityId = requireCommunity(caller);

        if (slotRepository.findByCommunityIdAndSlotNumber(communityId, req.slotNumber()).isPresent()) {
            throw new InvalidInputException("Slot number " + req.slotNumber() + " already exists in this community.");
        }

        ParkingSlot slot = ParkingSlot.builder()
                .community(caller.getCommunity())
                .slotNumber(req.slotNumber())
                .zone(req.zone())
                .floor(req.floor())
                .slotType(parseSlotType(req.slotType()))
                .notes(req.notes())
                .build();

        return toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public ParkingSlotResponse updateSlot(AppUser caller, Long slotId, ParkingSlotRequest req) {
        ParkingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", slotId));

        slot.setSlotNumber(req.slotNumber());
        slot.setZone(req.zone());
        slot.setFloor(req.floor());
        slot.setSlotType(parseSlotType(req.slotType()));
        slot.setNotes(req.notes());

        return toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public ParkingSlotResponse assignSlot(Long slotId, Long userId, AppUser caller) {
        ParkingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", slotId));

        if (slot.getStatus() == ParkingSlot.SlotStatus.OCCUPIED) {
            throw new InvalidInputException("Slot " + slot.getSlotNumber() + " is already occupied.");
        }

        slot.setAssignedTo(caller); // simplified: in real usage, resolve the target user
        slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
        return toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public ParkingSlotResponse releaseSlot(Long slotId) {
        ParkingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", slotId));

        slot.setAssignedTo(null);
        slot.setVehicleId(null);
        slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
        return toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public void deleteSlot(Long slotId) {
        if (!slotRepository.existsById(slotId)) {
            throw new ResourceNotFoundException("ParkingSlot", slotId);
        }
        List<ResidentVehicle> linked = vehicleRepository.findByParkingSlotId(slotId);
        for (ResidentVehicle v : linked) {
            v.setParkingSlot(null);
            vehicleRepository.save(v);
        }
        slotRepository.deleteById(slotId);
    }

    // ── Resident Vehicles ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResidentVehicleResponse> getVehicles(Long communityId) {
        return vehicleRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toVehicleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ResidentVehicleResponse> getMyVehicles(Long userId) {
        return vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toVehicleResponse).toList();
    }

    @Transactional
    public ResidentVehicleResponse registerVehicle(AppUser caller, ResidentVehicleRequest req) {
        Long communityId = requireCommunity(caller);

        if (vehicleRepository.findByCommunityIdAndNumberPlate(communityId, req.numberPlate().toUpperCase()).isPresent()) {
            throw new InvalidInputException("Vehicle " + req.numberPlate() + " is already registered in this community.");
        }

        ParkingSlot slot = null;
        if (req.parkingSlotId() != null) {
            slot = slotRepository.findById(req.parkingSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", req.parkingSlotId()));
        }

        ResidentVehicle vehicle = ResidentVehicle.builder()
                .community(caller.getCommunity())
                .owner(caller)
                .vehicleType(parseVehicleType(req.vehicleType()))
                .make(req.make())
                .model(req.model())
                .color(req.color())
                .numberPlate(req.numberPlate().toUpperCase())
                .parkingSlot(slot)
                .stickerNumber(req.stickerNumber())
                .primary(req.primary())
                .build();

        ResidentVehicle saved = vehicleRepository.save(vehicle);

        if (slot != null && slot.getStatus() == ParkingSlot.SlotStatus.AVAILABLE) {
            slot.setStatus(ParkingSlot.SlotStatus.OCCUPIED);
            slot.setAssignedTo(caller);
            slot.setVehicleId(saved.getId());
            slotRepository.save(slot);
        }

        return toVehicleResponse(saved);
    }

    @Transactional
    public ResidentVehicleResponse updateVehicle(AppUser caller, Long vehicleId, ResidentVehicleRequest req) {
        ResidentVehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentVehicle", vehicleId));

        vehicle.setVehicleType(parseVehicleType(req.vehicleType()));
        vehicle.setMake(req.make());
        vehicle.setModel(req.model());
        vehicle.setColor(req.color());
        vehicle.setNumberPlate(req.numberPlate().toUpperCase());
        vehicle.setStickerNumber(req.stickerNumber());
        vehicle.setPrimary(req.primary());

        if (req.parkingSlotId() != null) {
            ParkingSlot slot = slotRepository.findById(req.parkingSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", req.parkingSlotId()));
            vehicle.setParkingSlot(slot);
        } else {
            vehicle.setParkingSlot(null);
        }

        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        ResidentVehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentVehicle", vehicleId));

        if (vehicle.getParkingSlot() != null) {
            ParkingSlot slot = vehicle.getParkingSlot();
            slot.setStatus(ParkingSlot.SlotStatus.AVAILABLE);
            slot.setAssignedTo(null);
            slot.setVehicleId(null);
            slotRepository.save(slot);
        }

        vehicleRepository.delete(vehicle);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Long requireCommunity(AppUser caller) {
        if (caller.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        return caller.getCommunity().getId();
    }

    private ParkingSlot.SlotType parseSlotType(String raw) {
        if (raw == null || raw.isBlank()) return ParkingSlot.SlotType.COVERED;
        try { return ParkingSlot.SlotType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return ParkingSlot.SlotType.COVERED; }
    }

    private ResidentVehicle.VehicleType parseVehicleType(String raw) {
        if (raw == null || raw.isBlank()) return ResidentVehicle.VehicleType.CAR;
        try { return ResidentVehicle.VehicleType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResidentVehicle.VehicleType.CAR; }
    }

    private ParkingSlotResponse toSlotResponse(ParkingSlot slot) {
        ResidentVehicle linkedVehicle = null;
        if (slot.getVehicleId() != null) {
            linkedVehicle = vehicleRepository.findById(slot.getVehicleId()).orElse(null);
        }
        return ParkingSlotResponse.builder()
                .id(slot.getId())
                .slotNumber(slot.getSlotNumber())
                .zone(slot.getZone())
                .floor(slot.getFloor())
                .slotType(slot.getSlotType().name())
                .status(slot.getStatus().name())
                .assignedToId(slot.getAssignedTo() != null ? slot.getAssignedTo().getId() : null)
                .assignedToName(slot.getAssignedTo() != null ? slot.getAssignedTo().getFullName() : null)
                .vehicleId(slot.getVehicleId())
                .vehicleNumberPlate(linkedVehicle != null ? linkedVehicle.getNumberPlate() : null)
                .notes(slot.getNotes())
                .createdAt(slot.getCreatedAt())
                .build();
    }

    private ResidentVehicleResponse toVehicleResponse(ResidentVehicle v) {
        return ResidentVehicleResponse.builder()
                .id(v.getId())
                .ownerId(v.getOwner().getId())
                .ownerName(v.getOwner().getFullName())
                .vehicleType(v.getVehicleType().name())
                .make(v.getMake())
                .model(v.getModel())
                .color(v.getColor())
                .numberPlate(v.getNumberPlate())
                .parkingSlotId(v.getParkingSlot() != null ? v.getParkingSlot().getId() : null)
                .parkingSlotNumber(v.getParkingSlot() != null ? v.getParkingSlot().getSlotNumber() : null)
                .stickerNumber(v.getStickerNumber())
                .primary(v.isPrimary())
                .status(v.getStatus().name())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
