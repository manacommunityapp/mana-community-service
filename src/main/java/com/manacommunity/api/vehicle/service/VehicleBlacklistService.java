package com.manacommunity.api.vehicle.service;

import com.manacommunity.api.vehicle.entity.VehicleBlacklist;
import com.manacommunity.api.vehicle.repository.VehicleBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleBlacklistService {

    private final VehicleBlacklistRepository repo;

    @Transactional(readOnly = true)
    public List<VehicleBlacklist> getBlacklist(Long communityId) {
        return repo.findByCommunityIdAndIsActiveTrue(communityId);
    }

    @Transactional
    public VehicleBlacklist addToBlacklist(Long communityId, String plateNumber, String reason, Long addedByUserId) {
        return repo.save(VehicleBlacklist.builder()
                .communityId(communityId)
                .plateNumber(plateNumber.toUpperCase().trim())
                .reason(reason)
                .blacklistedByUserId(addedByUserId)
                .build());
    }

    @Transactional
    public void removeFromBlacklist(Long id) {
        VehicleBlacklist entry = repo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("VehicleBlacklist", id));
        entry.setIsActive(false);
        repo.save(entry);
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(Long communityId, String plateNumber) {
        return repo.existsByCommunityIdAndPlateNumberAndIsActiveTrue(communityId, plateNumber.toUpperCase().trim());
    }
}
