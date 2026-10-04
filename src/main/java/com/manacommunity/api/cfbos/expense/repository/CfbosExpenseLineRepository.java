package com.manacommunity.api.cfbos.expense.repository;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpenseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CfbosExpenseLineRepository extends JpaRepository<CfbosExpenseLine, Long> { List<CfbosExpenseLine> findByExpenseId(Long expenseId); }
