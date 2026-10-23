package com.manacommunity.api.repository;

import com.manacommunity.api.model.PersonalBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalBudgetRepository extends JpaRepository<PersonalBudget, Long> {

    List<PersonalBudget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    List<PersonalBudget> findByUserId(Long userId);

    Optional<PersonalBudget> findByIdAndUserId(Long id, Long userId);
}
