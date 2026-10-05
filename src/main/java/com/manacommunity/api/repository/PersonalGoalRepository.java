package com.manacommunity.api.repository;

import com.manacommunity.api.model.PersonalGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalGoalRepository extends JpaRepository<PersonalGoal, Long> {

    List<PersonalGoal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PersonalGoal> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PersonalGoal.GoalStatus status);

    Optional<PersonalGoal> findByIdAndUserId(Long id, Long userId);
}
