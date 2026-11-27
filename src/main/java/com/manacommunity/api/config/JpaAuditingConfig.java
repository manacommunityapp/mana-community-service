package com.manacommunity.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Activates Spring Data JPA repositories, entity scanning, and auditing.
 *
 * Separated from the main application class so that @WebMvcTest slices
 * (which don't load the full JPA context) don't register all JPA repositories
 * or fail looking for entityManagerFactory / auditorAware.
 *
 * The auditorAwareRef points to {@link SecurityAuditorAware}, which
 * resolves the current authenticated user ID from the SecurityContext.
 */
@Configuration
@EntityScan(basePackages = "com.manacommunity")
@EnableJpaRepositories(basePackages = "com.manacommunity")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
