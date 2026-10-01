package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalGoal;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalGoalRepository extends JpaRepository<PersonalGoal, String> {
    List<PersonalGoal> findByUserOrderByCreatedAtDesc(AppUser user);
    Optional<PersonalGoal> findByIdAndUser(String id, AppUser user);
}
