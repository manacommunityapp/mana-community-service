package com.manacommunity.api.wellness.service;

import com.manacommunity.api.wellness.dto.*;
import com.manacommunity.api.wellness.entity.*;
import com.manacommunity.api.wellness.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerProgramService {

    private final TrainerProgramRepository programRepo;
    private final TrainerProgramDayRepository dayRepo;
    private final UserProgramEnrollmentRepository enrollmentRepo;
    private final SportsWorkoutLinkRepository sportsLinkRepo;
    private final WorkoutService workoutService;

    // ── Programs ──

    public List<TrainerProgramDto> listPrograms(Long userId, String difficulty) {
        List<TrainerProgram> programs = difficulty != null
                ? programRepo.findByIsPublicTrueAndDifficultyOrderByCreatedAtDesc(difficulty)
                : programRepo.findByIsPublicTrueOrCreatedByUserIdOrderByCreatedAtDesc(userId);

        return programs.stream().map(p -> {
            TrainerProgramDto dto = toDto(p);
            enrollmentRepo.findByUserIdAndProgramId(userId, p.getId()).ifPresent(e -> {
                dto.setEnrolled(true);
                dto.setEnrollmentStatus(e.getStatus());
                dto.setCurrentWeek(e.getCurrentWeek());
                dto.setCurrentDay(e.getCurrentDay());
            });
            if (dto.getEnrolled() == null) dto.setEnrolled(false);
            return dto;
        }).collect(Collectors.toList());
    }

    public TrainerProgramDto getProgram(Long id, Long userId) {
        TrainerProgram program = programRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
        TrainerProgramDto dto = toDto(program);
        List<TrainerProgramDayDto> days = dayRepo.findByProgramIdOrderByWeekNumberAscDayNumberAsc(id)
                .stream().map(this::toDayDto).collect(Collectors.toList());
        dto.setDays(days);
        enrollmentRepo.findByUserIdAndProgramId(userId, id).ifPresent(e -> {
            dto.setEnrolled(true);
            dto.setEnrollmentStatus(e.getStatus());
            dto.setCurrentWeek(e.getCurrentWeek());
            dto.setCurrentDay(e.getCurrentDay());
        });
        if (dto.getEnrolled() == null) dto.setEnrolled(false);
        return dto;
    }

    @Transactional
    public TrainerProgramDto createProgram(Long userId, TrainerProgramDto req) {
        TrainerProgram p = TrainerProgram.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .difficulty(req.getDifficulty() != null ? req.getDifficulty() : "BEGINNER")
                .targetGoal(req.getTargetGoal())
                .durationWeeks(req.getDurationWeeks() != null ? req.getDurationWeeks() : 4)
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : false)
                .createdByUserId(userId)
                .build();
        programRepo.save(p);
        return toDto(p);
    }

    // ── Enrollment ──

    @Transactional
    public UserProgramEnrollmentDto enroll(Long userId, Long programId, LocalDate startDate) {
        TrainerProgram program = programRepo.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));

        Optional<UserProgramEnrollment> existing = enrollmentRepo.findByUserIdAndProgramId(userId, programId);
        UserProgramEnrollment enrollment;
        if (existing.isPresent()) {
            enrollment = existing.get();
            enrollment.setStatus("ACTIVE");
            enrollment.setStartDate(startDate != null ? startDate : LocalDate.now());
            enrollment.setCurrentWeek(1);
            enrollment.setCurrentDay(1);
            enrollment.setCompletedAt(null);
        } else {
            enrollment = UserProgramEnrollment.builder()
                    .userId(userId)
                    .programId(programId)
                    .startDate(startDate != null ? startDate : LocalDate.now())
                    .currentWeek(1)
                    .currentDay(1)
                    .status("ACTIVE")
                    .build();
        }
        enrollmentRepo.save(enrollment);
        return toEnrollmentDto(enrollment, program, userId);
    }

    public List<UserProgramEnrollmentDto> getMyEnrollments(Long userId) {
        return enrollmentRepo.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                .map(e -> {
                    TrainerProgram p = programRepo.findById(e.getProgramId()).orElse(null);
                    return toEnrollmentDto(e, p, userId);
                })
                .collect(Collectors.toList());
    }

    public UserProgramEnrollmentDto getTodaysEnrollment(Long userId) {
        return enrollmentRepo.findFirstByUserIdAndStatusOrderByEnrolledAtDesc(userId, "ACTIVE")
                .map(e -> {
                    TrainerProgram p = programRepo.findById(e.getProgramId()).orElse(null);
                    return toEnrollmentDto(e, p, userId);
                })
                .orElse(null);
    }

    @Transactional
    public UserProgramEnrollmentDto completeDay(Long userId, Long enrollmentId) {
        UserProgramEnrollment e = enrollmentRepo.findById(enrollmentId)
                .filter(en -> en.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        TrainerProgram program = programRepo.findById(e.getProgramId()).orElseThrow();

        int nextDay = e.getCurrentDay() + 1;
        int nextWeek = e.getCurrentWeek();
        if (nextDay > 7) {
            nextDay = 1;
            nextWeek++;
        }
        if (nextWeek > program.getDurationWeeks()) {
            e.setStatus("COMPLETED");
            e.setCompletedAt(OffsetDateTime.now());
        } else {
            e.setCurrentWeek(nextWeek);
            e.setCurrentDay(nextDay);
        }
        enrollmentRepo.save(e);
        return toEnrollmentDto(e, program, userId);
    }

    @Transactional
    public UserProgramEnrollmentDto pauseEnrollment(Long userId, Long enrollmentId) {
        UserProgramEnrollment e = enrollmentRepo.findById(enrollmentId)
                .filter(en -> en.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        e.setStatus("PAUSED");
        enrollmentRepo.save(e);
        TrainerProgram p = programRepo.findById(e.getProgramId()).orElse(null);
        return toEnrollmentDto(e, p, userId);
    }

    @Transactional
    public UserProgramEnrollmentDto resumeEnrollment(Long userId, Long enrollmentId) {
        UserProgramEnrollment e = enrollmentRepo.findById(enrollmentId)
                .filter(en -> en.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        e.setStatus("ACTIVE");
        enrollmentRepo.save(e);
        TrainerProgram p = programRepo.findById(e.getProgramId()).orElse(null);
        return toEnrollmentDto(e, p, userId);
    }

    // ── Sports–Workout bridge ──

    @Transactional
    public WorkoutLogDto logSportsWorkout(Long userId, SportsWorkoutRequest req) {
        // Prevent duplicate links for the same booking
        if (req.getBookingId() != null) {
            Optional<SportsWorkoutLink> existing = sportsLinkRepo.findByUserIdAndBookingId(userId, req.getBookingId());
            if (existing.isPresent() && existing.get().getWorkoutLogId() != null) {
                return workoutService.getWorkoutById(userId, existing.get().getWorkoutLogId());
            }
        }

        // Build a workout log from the sports booking data
        WorkoutLogDto workoutDto = new WorkoutLogDto();
        workoutDto.setExerciseName(req.getSportType() != null ? req.getSportType() : "Sports Activity");
        workoutDto.setExerciseCategory("SPORTS");
        workoutDto.setWorkoutDate(req.getWorkoutDate() != null ? LocalDate.parse(req.getWorkoutDate()) : LocalDate.now());
        workoutDto.setDurationMin(req.getDurationMin() != null ? req.getDurationMin() : 60);
        workoutDto.setSource("SPORTS_BOOKING");
        workoutDto.setNotes(req.getNotes());

        WorkoutLogDto saved = workoutService.logWorkout(userId, workoutDto);

        SportsWorkoutLink link = SportsWorkoutLink.builder()
                .userId(userId)
                .bookingId(req.getBookingId())
                .workoutLogId(saved.getId())
                .sportType(req.getSportType())
                .build();
        sportsLinkRepo.save(link);

        return saved;
    }

    // ── Mappers ──

    private TrainerProgramDto toDto(TrainerProgram p) {
        TrainerProgramDto dto = new TrainerProgramDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setDifficulty(p.getDifficulty());
        dto.setTargetGoal(p.getTargetGoal());
        dto.setDurationWeeks(p.getDurationWeeks());
        dto.setIsPublic(p.getIsPublic());
        dto.setCreatedByUserId(p.getCreatedByUserId());
        dto.setThumbnailUrl(p.getThumbnailUrl());
        return dto;
    }

    private TrainerProgramDayDto toDayDto(TrainerProgramDay d) {
        TrainerProgramDayDto dto = new TrainerProgramDayDto();
        dto.setId(d.getId());
        dto.setProgramId(d.getProgramId());
        dto.setWeekNumber(d.getWeekNumber());
        dto.setDayNumber(d.getDayNumber());
        dto.setDayName(d.getDayName());
        dto.setIsRestDay(d.getIsRestDay());
        dto.setFocus(d.getFocus());
        dto.setExercises(d.getExercises());
        dto.setTotalDurationMin(d.getTotalDurationMin());
        dto.setNotes(d.getNotes());
        return dto;
    }

    private UserProgramEnrollmentDto toEnrollmentDto(UserProgramEnrollment e, TrainerProgram p, Long userId) {
        UserProgramEnrollmentDto dto = new UserProgramEnrollmentDto();
        dto.setId(e.getId());
        dto.setProgramId(e.getProgramId());
        dto.setStartDate(e.getStartDate());
        dto.setCurrentWeek(e.getCurrentWeek());
        dto.setCurrentDay(e.getCurrentDay());
        dto.setStatus(e.getStatus());
        if (p != null) {
            dto.setProgramTitle(p.getTitle());
            dto.setProgramDifficulty(p.getDifficulty());
            dto.setProgramDurationWeeks(p.getDurationWeeks());
        }
        // Attach today's day workout
        if ("ACTIVE".equals(e.getStatus())) {
            dayRepo.findByProgramIdAndWeekNumberAndDayNumber(e.getProgramId(), e.getCurrentWeek(), e.getCurrentDay())
                    .ifPresent(day -> dto.setTodaysDay(toDayDto(day)));
        }
        return dto;
    }
}
