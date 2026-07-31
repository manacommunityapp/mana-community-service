package com.cpn.domain.company.repository;

import com.cpn.domain.company.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByCompanySlug(String companySlug);
}
