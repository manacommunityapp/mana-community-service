package com.manacommunity.api.vehicle.service;

import com.manacommunity.api.vehicle.dto.RegisterVehicleRequest;
import com.manacommunity.api.vehicle.entity.VehicleRegistration;
import com.manacommunity.api.vehicle.repository.VehicleRegistrationRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRegistrationRepository repo;

    public List<VehicleRegistration> getMyVehicles(AppUser user) {
        return repo.findByOwnerIdAndStatus(user.getId(), "ACTIVE");
    }

    public VehicleRegistration register(RegisterVehicleRequest req, AppUser user) {
        VehicleRegistration v = VehicleRegistration.builder()
                .owner(user)
                .community(user.getCommunity())
                .plateNumber(req.getPlateNumber() != null ? req.getPlateNumber().toUpperCase().trim() : null)
                .vehicleType(req.getVehicleType() != null ? req.getVehicleType() : "CAR")
                .make(req.getMake())
                .model(req.getModel())
                .color(req.getColor())
                .isGateWhitelisted(false)
                .status("ACTIVE")
                .build();
        return repo.save(v);
    }

    public void remove(Long id, AppUser user) {
        VehicleRegistration v = repo.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (!v.getOwner().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        v.setStatus("DELETED");
        repo.save(v);
    }

    public VehicleRegistration toggleWhitelist(Long id, boolean enabled, AppUser user) {
        VehicleRegistration v = repo.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (!v.getOwner().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        v.setIsGateWhitelisted(enabled);
        return repo.save(v);
    }
}
