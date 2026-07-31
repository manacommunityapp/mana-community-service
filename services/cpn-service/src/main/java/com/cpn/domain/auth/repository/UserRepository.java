package com.cpn.domain.auth.repository;

import com.cpn.domain.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    
    Page<User> findByTenantId(UUID tenantId, Pageable pageable);
    
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}
