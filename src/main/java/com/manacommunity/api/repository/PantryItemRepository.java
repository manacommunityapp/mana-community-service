package com.manacommunity.api.repository;

import com.manacommunity.api.model.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    List<PantryItem> findByUserIdOrderByNameAsc(Long userId);

    Optional<PantryItem> findByIdAndUserId(Long id, Long userId);

    List<PantryItem> findByUserIdAndExpiryDateBeforeOrderByExpiryDateAsc(Long userId, LocalDate date);
}
