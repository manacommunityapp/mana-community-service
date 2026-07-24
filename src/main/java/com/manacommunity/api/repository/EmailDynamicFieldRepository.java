package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailDynamicField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailDynamicFieldRepository extends JpaRepository<EmailDynamicField, Long> {

    List<EmailDynamicField> findByActiveTrueOrderByCategoryAscSortOrderAscFieldKeyAsc();

    Optional<EmailDynamicField> findByFieldKey(String fieldKey);
}
