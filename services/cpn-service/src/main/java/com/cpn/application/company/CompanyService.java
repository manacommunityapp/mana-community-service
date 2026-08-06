package com.cpn.application.company;

import com.cpn.domain.company.model.Company;
import com.cpn.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Company getCompanyBySlug(String slug) {
        return companyRepository.findByCompanySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    }

    @Transactional
    public Company createCompany(Company company) {
        if (company.getCompanySlug() == null || company.getCompanySlug().isEmpty()) {
            company.setCompanySlug(company.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "-"));
        }
        return companyRepository.save(company);
    }
}
