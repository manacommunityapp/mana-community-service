package com.cpn.infrastructure.security;

import com.cpn.domain.auth.model.User;
import com.cpn.domain.auth.repository.UserRepository;
import com.cpn.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String tenantIdStr = TenantContext.getTenantId();
        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        
        User user = null;
        if (tenantId != null) {
            user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found in tenant"));
        } else {
            // Fallback or handle cross-tenant admin users if needed
            throw new UsernameNotFoundException("Tenant context required");
        }

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return CustomUserDetails.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .enabled(user.isActive())
                .build();
    }
}
