package com.cpn.application.auth.service;

import com.cpn.application.auth.dto.AuthResponse;
import com.cpn.application.auth.dto.LoginRequest;
import com.cpn.application.auth.dto.RegisterRequest;
import com.cpn.domain.auth.model.Role;
import com.cpn.domain.auth.model.Tenant;
import com.cpn.domain.auth.model.User;
import com.cpn.domain.auth.repository.TenantRepository;
import com.cpn.domain.auth.repository.UserRepository;
import com.cpn.infrastructure.exception.CpnErrorCode;
import com.cpn.infrastructure.exception.CpnException;
import com.cpn.infrastructure.security.CustomUserDetails;
import com.cpn.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationProvider authenticationProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Tenant tenant = tenantRepository.findByTenantCode(request.tenantCode())
                .orElseThrow(() -> new CpnException(CpnErrorCode.TENANT_NOT_FOUND, "Tenant code not found"));

        if (userRepository.existsByEmailAndTenantId(request.email(), tenant.getId())) {
            throw new CpnException(CpnErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered in this tenant");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .isActive(true)
                .build();
        user.setTenantId(tenant.getId());

        user = userRepository.save(user);

        // Ideally fetch actual roles from DB
        CustomUserDetails userDetails = CustomUserDetails.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .authorities(Set.of())
                .build();

        return generateAuthResponse(user, userDetails, tenant);
    }

    public AuthResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findByTenantCode(request.tenantCode())
                .orElseThrow(() -> new CpnException(CpnErrorCode.TENANT_NOT_FOUND, "Tenant code not found"));

        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CpnException(CpnErrorCode.USER_NOT_FOUND));

        return generateAuthResponse(user, userDetails, tenant);
    }

    private AuthResponse generateAuthResponse(User user, CustomUserDetails userDetails, Tenant tenant) {
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, tenant.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600000L,
                user.getId(),
                user.getEmail(),
                tenant.getId(),
                roles
        );
    }
}
