package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("financePersonalBudgetRepository")
public interface PersonalBudgetRepository extends JpaRepository<PersonalBudget, String> {
    List<PersonalBudget> findByUserIdAndMonthOrderByCreatedAtAsc(Long userId, String month);
    Optional<PersonalBudget> findByUserIdAndCategoryIdAndMonth(Long userId, String categoryId, String month);
}
