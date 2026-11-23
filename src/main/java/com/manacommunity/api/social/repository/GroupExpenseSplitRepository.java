package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.GroupExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GroupExpenseSplitRepository extends JpaRepository<GroupExpenseSplit, Long> {
    List<GroupExpenseSplit> findByExpenseId(Long expenseId);
    Optional<GroupExpenseSplit> findByExpenseIdAndMemberId(Long expenseId, Long memberId);

    @Query("""
        SELECT COALESCE(SUM(s.amountOwed), 0)
        FROM GroupExpenseSplit s
        JOIN GroupExpense e ON e.id = s.expenseId
        WHERE s.memberId = :userId AND e.groupId = :groupId AND s.isSettled = false
    """)
    BigDecimal sumUnsettledOwedByUser(Long groupId, Long userId);

    @Query("""
        SELECT COALESCE(SUM(s.amountOwed), 0)
        FROM GroupExpenseSplit s
        JOIN GroupExpense e ON e.id = s.expenseId
        WHERE e.paidBy = :userId AND e.groupId = :groupId AND s.isSettled = false AND s.memberId <> :userId
    """)
    BigDecimal sumUnsettledOwedToUser(Long groupId, Long userId);
}
