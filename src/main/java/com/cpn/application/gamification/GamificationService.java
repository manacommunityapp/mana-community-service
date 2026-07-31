package com.cpn.application.gamification;

import com.cpn.domain.gamification.model.GamificationBadge;
import com.cpn.domain.gamification.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final BadgeRepository badgeRepository;

    @Transactional(readOnly = true)
    public List<GamificationBadge> getUserBadges(UUID userId) {
        return badgeRepository.findByUserId(userId);
    }

    @Transactional
    public GamificationBadge awardBadge(GamificationBadge badge) {
        badge.setUnlocked(true);
        return badgeRepository.save(badge);
    }
}
