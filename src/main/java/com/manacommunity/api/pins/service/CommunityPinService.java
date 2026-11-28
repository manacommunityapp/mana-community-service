package com.manacommunity.api.pins.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.pins.dto.CommunityPinDto;
import com.manacommunity.api.pins.dto.CreatePinRequest;
import com.manacommunity.api.pins.entity.CommunityPin;
import com.manacommunity.api.pins.repository.CommunityPinRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPinService {

    private static final String CACHE_KEY_PREFIX = "pins:active:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final CommunityPinRepository pinRepository;
    private final CommunityRepository communityRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CommunityPinDto> getActivePins(Long communityId, String category) {
        if (communityId == null) {
            return List.of();
        }

        String cacheKey = CACHE_KEY_PREFIX + communityId + (category != null ? ":" + category : "");
        if (redisTemplate != null && objectMapper != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    return objectMapper.readValue(cachedJson, new TypeReference<List<CommunityPinDto>>() {});
                }
            } catch (Exception e) {
                log.debug("Redis cache read failed for pins in community {}: {}", communityId, e.getMessage());
            }
        }

        Instant now = Instant.now();
        List<CommunityPin> pins;
        if (category != null && !category.isBlank()) {
            pins = pinRepository.findByCommunityIdAndCategoryAndExpiresAtAfterOrderByCreatedAtDesc(
                    communityId, category.trim().toUpperCase(), now);
        } else {
            pins = pinRepository.findByCommunityIdAndExpiresAtAfterOrderByCreatedAtDesc(communityId, now);
        }

        List<CommunityPinDto> dtoList = pins.stream()
                .map(CommunityPinDto::fromEntity)
                .collect(Collectors.toList());

        if (redisTemplate != null && objectMapper != null) {
            try {
                String json = objectMapper.writeValueAsString(dtoList);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            } catch (Exception e) {
                log.debug("Redis cache write failed for pins in community {}: {}", communityId, e.getMessage());
            }
        }
        return dtoList;
    }

    @Transactional
    public CommunityPinDto createPin(Long communityId, AppUser author, CreatePinRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId.toString()));

        int hours = request.getDurationHours() != null ? Math.min(Math.max(request.getDurationHours(), 1), 168) : 24;
        Instant expiresAt = Instant.now().plus(Duration.ofHours(hours));

        String flat = author.getFlatNo();
        if (flat == null && author.getBlock() != null) {
            flat = author.getBlock();
        }

        CommunityPin pin = CommunityPin.builder()
                .community(community)
                .author(author)
                .authorName(author.getFullName() != null ? author.getFullName() : "Resident")
                .flatNumber(flat)
                .category(request.getCategory().trim().toUpperCase())
                .content(request.getContent().trim())
                .imageUrl(request.getImageUrl())
                .externalLink(request.getExternalLink())
                .expiresAt(expiresAt)
                .isClosed(false)
                .likesCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CommunityPin saved = pinRepository.save(pin);
        evictCache(communityId);
        return CommunityPinDto.fromEntity(saved);
    }

    @Transactional
    public CommunityPinDto closePin(Long pinId, Long communityId, AppUser user) {
        CommunityPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityPin", "id", pinId.toString()));

        boolean isAuthor = pin.getAuthor() != null && pin.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.hasRole("ADMIN") || user.hasRole("SUPER_ADMIN") || user.hasRole("COMMUNITY_ADMIN");
        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("Only the author or community admin may close this pin");
        }

        pin.setIsClosed(true);
        pin.setClosedAt(Instant.now());
        pin.setUpdatedAt(Instant.now());
        CommunityPin saved = pinRepository.save(pin);
        evictCache(communityId);
        return CommunityPinDto.fromEntity(saved);
    }

    @Transactional
    public CommunityPinDto toggleLike(Long pinId, Long communityId) {
        pinRepository.incrementLikes(pinId);
        CommunityPin updated = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityPin", "id", pinId.toString()));
        evictCache(communityId);
        return CommunityPinDto.fromEntity(updated);
    }

    private void evictCache(Long communityId) {
        if (redisTemplate != null) {
            try {
                // Delete all cached category variations for this community
                var keys = redisTemplate.keys(CACHE_KEY_PREFIX + communityId + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                log.debug("Redis cache eviction failed for community pins {}: {}", communityId, e.getMessage());
            }
        }
    }
}
