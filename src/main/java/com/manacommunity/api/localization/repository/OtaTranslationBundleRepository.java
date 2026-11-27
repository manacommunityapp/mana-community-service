package com.manacommunity.api.localization.repository;

import com.manacommunity.api.localization.entity.OtaTranslationBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtaTranslationBundleRepository extends JpaRepository<OtaTranslationBundleEntity, UUID> {

    List<OtaTranslationBundleEntity> findByLocaleAndActiveTrue(String locale);

    Optional<OtaTranslationBundleEntity> findTopByLocaleAndNamespaceAndActiveTrueOrderByPublishedAtDesc(
            String locale, String namespace);
}
