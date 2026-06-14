package com.manacommunity.api.config;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.manacommunity.api.security.MockJwtFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * BUG FIXES applied:
 *
 * 1. SecurityConfig was package-private — Spring Security requires public
 *    configuration beans to be visible. Made public.
 *
 * 2. No UserDetailsService was defined. Spring Security needs one to load
 *    authenticated users via @AuthenticationPrincipal. Without it, all
 *    protected endpoints would fail with 403 even after login.
 *    Added a UserDetailsService bean that loads AppUser by email and wraps
 *    it in our UserPrincipal.
 *
 * 3. pom.xml has `spring-boot-starter-webmvc` AND `spring-boot-starter-web`
 *    — these conflict/duplicate. The web starter already includes webmvc.
 *    Note left for developer; the webmvc dependency should be removed from pom.
 *
 * 4. Deprecated chained API (http.cors().and().csrf()) used — updated to
 *    Spring Security 6.x lambda DSL.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig { // BUG FIX: was package-private

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private MockJwtFilter mockJwtFilter;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * BUG FIX: Missing UserDetailsService — required for Spring Security to
     * authenticate users and populate @AuthenticationPrincipal.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
            return new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(Arrays.asList("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // BUG FIX: Updated to Spring Security 6 lambda DSL (non-deprecated)
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(mockJwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui.html", 
                    "/swagger-ui/**", 
                    "/v3/api-docs/**", 
                    "/swagger-resources/**", 
                    "/webjars/**"
                ).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/communities/**").permitAll()
                .requestMatchers("/api/admin/seed/**").permitAll()
                .requestMatchers("/api/admin/email/**").permitAll()
                .requestMatchers("/api/tournament/**").permitAll()
                .requestMatchers("/*.html", "/css/**", "/js/**", "/static/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }



}
