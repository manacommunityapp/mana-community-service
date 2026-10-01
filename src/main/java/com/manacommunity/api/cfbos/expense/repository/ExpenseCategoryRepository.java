package com.manacommunity.api.cfbos.expense.repository;
import com.manacommunity.api.cfbos.expense.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> { Optional<ExpenseCategory> findByCode(String code); }
