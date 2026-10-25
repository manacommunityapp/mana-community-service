package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.TrainerSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainerSlotRepository extends JpaRepository<TrainerSlot, Long> {

    List<TrainerSlot> findByTrainer_IdAndStatusOrderBySlotDateAscStartTimeAsc(Long trainerId, String status);

    List<TrainerSlot> findByCommunityIdAndSportAndSlotDateBetweenAndStatusOrderBySlotDateAscStartTimeAsc(
            Long communityId, String sport, LocalDate from, LocalDate to, String status);

    List<TrainerSlot> findByCommunityIdAndSlotDateBetweenAndStatusOrderBySlotDateAscStartTimeAsc(
            Long communityId, LocalDate from, LocalDate to, String status);
}
