package com.manacommunity.api.user.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Utility service to resolve the full AppUser entity from
 * a Spring Security UserPrincipal (JWT / session).
 *
 * Inject this into any controller or service that needs the
 * complete logged-in user object (community, role, flat, etc.).
 */
@Service
@RequiredArgsConstructor
public class LoggedInUserService {

    private final AppUserRepository userRepository;

    /**
     * Resolves the full AppUser entity from the authenticated principal.
     *
     * @param principal the UserPrincipal injected by Spring Security
     * @return the AppUser entity with all fields loaded
     * @throws ResourceNotFoundException if the user ID no longer exists in the DB
     */
    public AppUser resolve(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId().toString()));
    }
}
