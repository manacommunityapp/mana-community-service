package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.GroupExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, Long> {
    List<GroupExpense> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
