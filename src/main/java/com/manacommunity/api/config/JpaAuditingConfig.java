package com.manacommunity.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates Spring Data JPA auditing.
 *
 * Separated from the main application class so that @WebMvcTest slices
 * (which don't load the full JPA context) don't fail looking for the
 * auditorAware bean.
 *
 * The auditorAwareRef points to {@link SecurityAuditorAware}, which
 * resolves the current authenticated user ID from the SecurityContext.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
