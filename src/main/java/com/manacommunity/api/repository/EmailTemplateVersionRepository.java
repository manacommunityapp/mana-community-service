package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateVersionRepository extends JpaRepository<EmailTemplateVersion, Long> {

    List<EmailTemplateVersion> findByTemplate_IdOrderByVersionDesc(Long templateId);
}
