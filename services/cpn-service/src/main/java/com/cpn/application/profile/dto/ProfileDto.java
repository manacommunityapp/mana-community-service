package com.cpn.application.profile.dto;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public record ProfileDto(
        UUID id,
        UUID userId,
        String headline,
        String summary,
        String location,
        boolean isOpenToWork,
        Integer aiCompletionScore,
        List<SkillDto> skills,
        List<ExperienceDto> experiences,
        List<EducationDto> educations
) {
    public record SkillDto(UUID id, String skillName, Integer yearsOfExperience, Integer proficiencyLevel) {}
    
    public record ExperienceDto(UUID id, String companyName, String designation, String location, LocalDate startDate, LocalDate endDate, boolean isCurrentJob, String description) {}
    
    public record EducationDto(UUID id, String institutionName, String degree, String fieldOfStudy, LocalDate startDate, LocalDate endDate, String grade) {}
}
