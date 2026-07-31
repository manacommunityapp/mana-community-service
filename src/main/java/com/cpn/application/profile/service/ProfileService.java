package com.cpn.application.profile.service;

import com.cpn.application.profile.dto.ProfileDto;
import com.cpn.domain.profile.model.Profile;
import com.cpn.domain.profile.repository.ProfileRepository;
import com.cpn.infrastructure.exception.CpnErrorCode;
import com.cpn.infrastructure.exception.CpnException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileDto getProfile(UUID userId, UUID tenantId) {
        Profile profile = profileRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new CpnException(CpnErrorCode.PROFILE_NOT_FOUND));
        return mapToDto(profile);
    }

    public Page<ProfileDto> searchProfiles(String skill, UUID tenantId, Pageable pageable) {
        if (skill != null && !skill.isBlank()) {
            return profileRepository.searchBySkillAndTenantId(skill, tenantId, pageable)
                    .map(this::mapToDto);
        }
        return profileRepository.findByTenantIdAndIsOpenToWorkTrue(tenantId, pageable)
                .map(this::mapToDto);
    }
    
    @Transactional
    public void updateAiSkillScore(UUID profileId, int score) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CpnException(CpnErrorCode.PROFILE_NOT_FOUND));
        profile.setAiCompletionScore(score);
        profileRepository.save(profile);
    }

    // Dummy mapper, in real project use MapStruct
    private ProfileDto mapToDto(Profile p) {
        return new ProfileDto(
            p.getId(), p.getUserId(), p.getHeadline(), p.getSummary(), p.getLocation(), p.isOpenToWork(), p.getAiCompletionScore(),
            p.getSkills().stream().map(s -> new ProfileDto.SkillDto(s.getId(), s.getSkillName(), s.getYearsOfExperience(), s.getProficiencyLevel())).collect(Collectors.toList()),
            p.getExperiences().stream().map(e -> new ProfileDto.ExperienceDto(e.getId(), e.getCompanyName(), e.getDesignation(), e.getLocation(), e.getStartDate(), e.getEndDate(), e.isCurrentJob(), e.getDescription())).collect(Collectors.toList()),
            p.getEducations().stream().map(e -> new ProfileDto.EducationDto(e.getId(), e.getInstitutionName(), e.getDegree(), e.getFieldOfStudy(), e.getStartDate(), e.getEndDate(), e.getGrade())).collect(Collectors.toList())
        );
    }
}
