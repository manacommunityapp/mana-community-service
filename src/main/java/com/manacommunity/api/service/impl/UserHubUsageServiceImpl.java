package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.UserHubUsage;
import com.manacommunity.api.repository.UserHubUsageRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.service.UserHubUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHubUsageServiceImpl implements UserHubUsageService {

    private final UserHubUsageRepository hubUsageRepo;
    private final AppUserRepository appUserRepo;

    @Override
    @Transactional
    public void trackClick(Long userId, String hubId, String hubLabel) {
        Optional<UserHubUsage> existing = hubUsageRepo.findByUserIdAndHubId(userId, hubId);

        if (existing.isPresent()) {
            UserHubUsage usage = existing.get();
            usage.setClickCount(usage.getClickCount() + 1);
            usage.setLastUsedAt(LocalDateTime.now());
            if (hubLabel != null) {
                usage.setHubLabel(hubLabel);
            }
            hubUsageRepo.save(usage);
        } else {
            AppUser user = appUserRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            UserHubUsage usage = UserHubUsage.builder()
                    .user(user)
                    .hubId(hubId)
                    .hubLabel(hubLabel)
                    .clickCount(1)
                    .build();
            hubUsageRepo.save(usage);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopHubs(Long userId, int limit) {
        List<UserHubUsage> allUsage = hubUsageRepo.findByUserIdOrderByClickCountDesc(userId);

        if (allUsage.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        int maxClicks = allUsage.stream().mapToInt(UserHubUsage::getClickCount).max().orElse(1);

        return allUsage.stream()
                .map(u -> {
                    double freqScore = (double) u.getClickCount() / maxClicks;
                    long hoursAgo = ChronoUnit.HOURS.between(u.getLastUsedAt(), now);
                    double recencyScore = Math.max(0, 1.0 - (hoursAgo / 720.0));
                    double score = (freqScore * 0.7) + (recencyScore * 0.3);

                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("hubId", u.getHubId());
                    map.put("hubLabel", u.getHubLabel());
                    map.put("clickCount", u.getClickCount());
                    map.put("lastUsedAt", u.getLastUsedAt().toString());
                    map.put("score", Math.round(score * 100.0) / 100.0);
                    return map;
                })
                .sorted((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
