package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalBillRepository extends JpaRepository<PersonalBill, String> {
    List<PersonalBill> findByUserIdOrderByDueDateAsc(Long userId);
    Optional<PersonalBill> findByIdAndUserId(String id, Long userId);
}
