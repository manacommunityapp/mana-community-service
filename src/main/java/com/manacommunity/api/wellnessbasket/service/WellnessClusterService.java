package com.manacommunity.api.wellnessbasket.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.wellnessbasket.dto.WellnessClusterDto;
import com.manacommunity.api.wellnessbasket.entity.WellnessCommunityCluster;
import com.manacommunity.api.wellnessbasket.repository.WellnessCommunityClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessClusterService {

    private final WellnessCommunityClusterRepository clusterRepository;

    @Transactional
    public WellnessCommunityCluster getOrCreateCluster(String societyName, String city, String block) {
        String effectiveSociety = (societyName != null && !societyName.isBlank()) ? societyName.trim() : "Mana Premiere Residences";
        return clusterRepository.findBySocietyNameIgnoreCase(effectiveSociety)
                .orElseGet(() -> {
                    WellnessCommunityCluster cluster = new WellnessCommunityCluster();
                    cluster.setSocietyName(effectiveSociety);
                    cluster.setCity(city != null && !city.isBlank() ? city.trim() : "Hyderabad");
                    cluster.setBlock(block);
                    cluster.setStatus("PENDING");
                    cluster.setCurrentSubscriberCount(0);
                    cluster.setActivationThreshold(15);
                    cluster.setMaxCapacity(80);
                    cluster.setDeliveryStartTime(LocalTime.of(6, 0));
                    cluster.setDeliveryEndTime(LocalTime.of(8, 0));
                    WellnessCommunityCluster saved = clusterRepository.save(cluster);
                    log.info("Created new delivery cluster {} for society '{}'", saved.getId(), effectiveSociety);
                    return saved;
                });
    }

    @Transactional
    public void incrementSubscriberCount(Long clusterId) {
        if (clusterId == null) return;
        clusterRepository.findById(clusterId).ifPresent(cluster -> {
            int newCount = cluster.getCurrentSubscriberCount() + 1;
            cluster.setCurrentSubscriberCount(newCount);
            if ("PENDING".equals(cluster.getStatus()) && newCount >= cluster.getActivationThreshold()) {
                cluster.setStatus("ACTIVE");
                log.info("Cluster {} for '{}' REACHED threshold ({}) and is now ACTIVE!",
                        cluster.getId(), cluster.getSocietyName(), newCount);
            }
            clusterRepository.save(cluster);
        });
    }

    @Transactional
    public void decrementSubscriberCount(Long clusterId) {
        if (clusterId == null) return;
        clusterRepository.findById(clusterId).ifPresent(cluster -> {
            int newCount = Math.max(0, cluster.getCurrentSubscriberCount() - 1);
            cluster.setCurrentSubscriberCount(newCount);
            clusterRepository.save(cluster);
        });
    }

    public List<WellnessClusterDto> getAllClusters() {
        return clusterRepository.findAll().stream().map(this::toDto).toList();
    }

    public Optional<WellnessClusterDto> getClusterBySociety(String societyName) {
        return clusterRepository.findBySocietyNameIgnoreCase(societyName).map(this::toDto);
    }

    public WellnessClusterDto getClusterById(Long clusterId) {
        return clusterRepository.findById(clusterId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("WellnessCommunityCluster", clusterId));
    }

    @Transactional
    public WellnessClusterDto updateClusterStatus(Long clusterId, String status) {
        WellnessCommunityCluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("WellnessCommunityCluster", clusterId));
        cluster.setStatus(status.toUpperCase());
        return toDto(clusterRepository.save(cluster));
    }

    public WellnessClusterDto toDto(WellnessCommunityCluster cluster) {
        boolean isActivated = "ACTIVE".equalsIgnoreCase(cluster.getStatus()) ||
                cluster.getCurrentSubscriberCount() >= cluster.getActivationThreshold();
        int needed = Math.max(0, cluster.getActivationThreshold() - cluster.getCurrentSubscriberCount());

        return WellnessClusterDto.builder()
                .id(cluster.getId())
                .societyName(cluster.getSocietyName())
                .block(cluster.getBlock())
                .city(cluster.getCity())
                .lat(cluster.getLat())
                .lng(cluster.getLng())
                .status(cluster.getStatus())
                .currentSubscriberCount(cluster.getCurrentSubscriberCount())
                .activationThreshold(cluster.getActivationThreshold())
                .maxCapacity(cluster.getMaxCapacity())
                .deliveryStartTime(cluster.getDeliveryStartTime())
                .deliveryEndTime(cluster.getDeliveryEndTime())
                .isActivated(isActivated)
                .subscribersNeededForActivation(needed)
                .build();
    }
}
