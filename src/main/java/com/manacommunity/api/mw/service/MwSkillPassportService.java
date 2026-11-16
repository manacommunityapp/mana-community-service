package com.manacommunity.api.mw.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.mw.dto.*;
import com.manacommunity.api.mw.entity.*;
import com.manacommunity.api.mw.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MwSkillPassportService {

    private final MwSkillPassportRepository passportRepo;
    private final MwPassportSkillRepository skillRepo;
    private final MwPassportCertificationRepository certRepo;
    private final MwPassportWorkHistoryRepository workHistoryRepo;

    public MwSkillPassport getOrCreatePassport(Long manaUserId) {
        return passportRepo.findByManaUserId(manaUserId)
                .orElseGet(() -> passportRepo.save(
                        MwSkillPassport.builder().manaUserId(manaUserId).build()
                ));
    }

    public MwSkillPassport getMyPassport(Long manaUserId) {
        return getOrCreatePassport(manaUserId);
    }

    public MwSkillPassport updatePassport(Long manaUserId, PassportUpdateRequest req) {
        MwSkillPassport passport = getOrCreatePassport(manaUserId);
        if (req.getBio() != null) passport.setBio(req.getBio());
        if (req.getLocation() != null) passport.setLocation(req.getLocation());
        if (req.getPreferredRoles() != null) passport.setPreferredRoles(req.getPreferredRoles());
        if (req.getAvailability() != null) passport.setAvailability(req.getAvailability());
        if (req.getIsPublic() != null) passport.setIsPublic(req.getIsPublic());
        return passportRepo.save(passport);
    }

    public MwPassportSkill addSkill(Long manaUserId, SkillRequest req) {
        MwSkillPassport passport = getOrCreatePassport(manaUserId);
        MwPassportSkill skill = MwPassportSkill.builder()
                .passport(passport)
                .skillName(req.getSkillName())
                .category(req.getCategory())
                .proficiency(req.getProficiency())
                .yearsExp(req.getYearsExp())
                .build();
        return skillRepo.save(skill);
    }

    public void removeSkill(Long manaUserId, Long skillId) {
        MwPassportSkill skill = skillRepo.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        if (!skill.getPassport().getManaUserId().equals(manaUserId)) {
            throw new AccessDeniedException("Not your skill");
        }
        skillRepo.delete(skill);
    }

    public MwPassportCertification addCertification(Long manaUserId, CertificationRequest req) {
        MwSkillPassport passport = getOrCreatePassport(manaUserId);
        MwPassportCertification cert = MwPassportCertification.builder()
                .passport(passport)
                .name(req.getName())
                .issuingBody(req.getIssuingBody())
                .issueDate(req.getIssueDate())
                .expiryDate(req.getExpiryDate())
                .credentialUrl(req.getCredentialUrl())
                .build();
        return certRepo.save(cert);
    }

    public void removeCertification(Long manaUserId, Long certId) {
        MwPassportCertification cert = certRepo.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found"));
        if (!cert.getPassport().getManaUserId().equals(manaUserId)) {
            throw new AccessDeniedException("Not your certification");
        }
        certRepo.delete(cert);
    }

    public MwPassportWorkHistory addWorkHistory(Long manaUserId, WorkHistoryRequest req) {
        MwSkillPassport passport = getOrCreatePassport(manaUserId);
        MwPassportWorkHistory wh = MwPassportWorkHistory.builder()
                .passport(passport)
                .employerName(req.getEmployerName())
                .role(req.getRole())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isCurrent(req.getIsCurrent())
                .build();
        return workHistoryRepo.save(wh);
    }

    public void removeWorkHistory(Long manaUserId, Long whId) {
        MwPassportWorkHistory wh = workHistoryRepo.findById(whId)
                .orElseThrow(() -> new ResourceNotFoundException("Work history not found"));
        if (!wh.getPassport().getManaUserId().equals(manaUserId)) {
            throw new AccessDeniedException("Not your work history");
        }
        workHistoryRepo.delete(wh);
    }

    public MwSkillPassport getPublicPassport(Long manaUserId) {
        MwSkillPassport passport = passportRepo.findByManaUserId(manaUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Passport not found"));
        if (!Boolean.TRUE.equals(passport.getIsPublic())) {
            throw new AccessDeniedException("This passport is not public");
        }
        return passport;
    }

    public List<MwPassportSkill> getUserSkills(Long manaUserId) {
        return passportRepo.findByManaUserId(manaUserId)
                .map(p -> skillRepo.findByPassportId(p.getId()))
                .orElse(List.of());
    }

    public boolean hasSkill(Long manaUserId, String skillName) {
        return passportRepo.findByManaUserId(manaUserId)
                .map(p -> !skillRepo.findByPassportIdAndSkillNameContainingIgnoreCase(p.getId(), skillName).isEmpty())
                .orElse(false);
    }

    public List<MwPassportCertification> getExpiringCertifications(Long manaUserId, int withinDays) {
        LocalDate cutoff = LocalDate.now().plusDays(withinDays);
        return passportRepo.findByManaUserId(manaUserId)
                .map(p -> certRepo.findByPassportIdAndExpiryDateBefore(p.getId(), cutoff))
                .orElse(List.of());
    }
}
