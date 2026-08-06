package com.cpn.application.profile;

import com.cpn.domain.profile.model.Profile;
import com.cpn.domain.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public Profile getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
    }

    @Transactional
    public Profile updateProfile(UUID userId, Profile updated) {
        Profile existing = getProfileByUserId(userId);
        existing.setHeadline(updated.getHeadline());
        existing.setSummary(updated.getSummary());
        existing.setLocation(updated.getLocation());
        existing.setOpenToWork(updated.isOpenToWork());
        existing.setAiCompletionScore(calculateSkillScore(existing));
        return profileRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Profile> searchProfiles(String query) {
        return profileRepository.findAll();
    }

    private Profile createDefaultProfile(UUID userId) {
        Profile p = Profile.builder()
                .userId(userId)
                .headline("Software Engineer | Community Member")
                .summary("Passionate tech professional focused on building modern solutions.")
                .location("Bangalore, India")
                .isOpenToWork(true)
                .aiCompletionScore(85)
                .build();
        return profileRepository.save(p);
    }

    private Integer calculateSkillScore(Profile p) {
        int score = 50;
        if (p.getHeadline() != null && !p.getHeadline().isEmpty()) score += 15;
        if (p.getSummary() != null && !p.getSummary().isEmpty()) score += 15;
        if (p.getLocation() != null && !p.getLocation().isEmpty()) score += 10;
        if (p.getSkills() != null && !p.getSkills().isEmpty()) score += 10;
        return Math.min(score, 100);
    }
}
