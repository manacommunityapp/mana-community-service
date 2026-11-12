package com.manacommunity.api.mw.dto;

import com.manacommunity.api.mw.entity.MwSkillPassport;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MwPassportPublicResponse {

    private Long manaUserId;
    private String bio;
    private String location;
    private String preferredRoles;
    private String availability;
    private Boolean isPublic;
    private List<SkillView> skills;
    private List<CertView> certifications;
    private List<WorkHistoryView> workHistory;

    @Getter
    @Builder
    public static class SkillView {
        private Long id;
        private String skillName;
        private String category;
        private String proficiency;
        private BigDecimal yearsExp;
        private Boolean isVerified;
    }

    @Getter
    @Builder
    public static class CertView {
        private Long id;
        private String name;
        private String issuingBody;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private String credentialUrl;
        private Boolean isVerified;
    }

    @Getter
    @Builder
    public static class WorkHistoryView {
        private Long id;
        private String employerName;
        private String role;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
        private Boolean isManaVerified;
    }

    public static MwPassportPublicResponse from(MwSkillPassport p) {
        return MwPassportPublicResponse.builder()
                .manaUserId(p.getManaUserId())
                .bio(p.getBio())
                .location(p.getLocation())
                .preferredRoles(p.getPreferredRoles())
                .availability(p.getAvailability())
                .isPublic(p.getIsPublic())
                .skills(p.getSkills() == null ? List.of() : p.getSkills().stream()
                        .map(s -> SkillView.builder()
                                .id(s.getId())
                                .skillName(s.getSkillName())
                                .category(s.getCategory())
                                .proficiency(s.getProficiency())
                                .yearsExp(s.getYearsExp())
                                .isVerified(s.getIsVerified())
                                .build())
                        .collect(Collectors.toList()))
                .certifications(p.getCertifications() == null ? List.of() : p.getCertifications().stream()
                        .map(c -> CertView.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .issuingBody(c.getIssuingBody())
                                .issueDate(c.getIssueDate())
                                .expiryDate(c.getExpiryDate())
                                .credentialUrl(c.getCredentialUrl())
                                .isVerified(c.getIsVerified())
                                .build())
                        .collect(Collectors.toList()))
                .workHistory(p.getWorkHistory() == null ? List.of() : p.getWorkHistory().stream()
                        .map(w -> WorkHistoryView.builder()
                                .id(w.getId())
                                .employerName(w.getEmployerName())
                                .role(w.getRole())
                                .startDate(w.getStartDate())
                                .endDate(w.getEndDate())
                                .isCurrent(w.getIsCurrent())
                                .isManaVerified(w.getIsManaVerified())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
