package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.SmsTemplate;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {

    Optional<SmsTemplate> findByTemplateCodeAndLanguage(String templateCode, SmsLanguage language);

    List<SmsTemplate> findByTemplateCode(String templateCode);

    List<SmsTemplate> findByStatus(TemplateStatus status);

    boolean existsByTemplateCodeAndLanguage(String templateCode, SmsLanguage language);

    /** Finds active template for code+language, falling back to EN if the requested language is absent. */
    @Query("""
            SELECT t FROM SmsTemplate t
            WHERE t.templateCode = :code
              AND t.status = com.manacommunity.api.notification.enums.TemplateStatus.ACTIVE
              AND t.language = :lang
            """)
    Optional<SmsTemplate> findActiveByCodeAndLanguage(
            @Param("code") String code, @Param("lang") SmsLanguage lang);
}
