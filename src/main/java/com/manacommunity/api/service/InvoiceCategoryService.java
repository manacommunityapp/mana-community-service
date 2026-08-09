package com.manacommunity.api.service;

import com.manacommunity.api.dto.InvoiceCategoryDto;
import com.manacommunity.api.model.InvoiceCategory;
import com.manacommunity.api.repository.InvoiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceCategoryService {

    private final InvoiceCategoryRepository repository;

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Venue Rental",
            "Food & Catering",
            "Decor & Stage",
            "Audio & Visual",
            "Security & Housekeeping",
            "Marketing & Banners",
            "Printing & Stationery",
            "Licenses & Permits",
            "Miscellaneous"
    );

    @Transactional
    public List<InvoiceCategoryDto> listCategories(Long communityId) {
        List<InvoiceCategory> list;
        if (communityId != null) {
            list = repository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId);
        } else {
            list = repository.findByActiveTrueOrderByNameAsc();
        }

        // Seed defaults if empty
        if (list.isEmpty()) {
            log.info("Seeding default invoice categories into database...");
            for (String catName : DEFAULT_CATEGORIES) {
                String code = catName.toUpperCase().replace(" & ", "_").replace(" ", "_");
                repository.save(InvoiceCategory.builder()
                        .communityId(communityId)
                        .name(catName)
                        .code(code)
                        .active(true)
                        .build());
            }
            list = communityId != null
                    ? repository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId)
                    : repository.findByActiveTrueOrderByNameAsc();
        }

        return list.stream().map(InvoiceCategoryDto::from).toList();
    }

    @Transactional
    public InvoiceCategoryDto saveCategory(InvoiceCategoryDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        String name = dto.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            log.info("Category '{}' already exists.", name);
        }

        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = name.toUpperCase().replace(" & ", "_").replace(" ", "_");
        }

        InvoiceCategory category = InvoiceCategory.builder()
                .communityId(dto.getCommunityId())
                .name(name)
                .code(code)
                .description(dto.getDescription())
                .active(true)
                .build();

        return InvoiceCategoryDto.from(repository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        repository.findById(id).ifPresent(c -> {
            c.setActive(false);
            repository.save(c);
        });
    }
}
