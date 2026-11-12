package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.TrainerProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainerProgramRepository extends JpaRepository<TrainerProgram, Long> {
    List<TrainerProgram> findByIsPublicTrueOrCreatedByUserIdOrderByCreatedAtDesc(Long userId);
    List<TrainerProgram> findByIsPublicTrueAndDifficultyOrderByCreatedAtDesc(String difficulty);
    List<TrainerProgram> findByIsPublicTrueOrderByCreatedAtDesc();
}
