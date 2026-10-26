package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsExternalFormFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsExternalFormFieldMappingRepository extends JpaRepository<SportsExternalFormFieldMapping, Long> {

    List<SportsExternalFormFieldMapping> findByFormId(Long formId);

    void deleteByFormId(Long formId);
}
