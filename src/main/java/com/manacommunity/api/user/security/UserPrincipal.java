package com.manacommunity.api.user.security;

import com.manacommunity.api.user.model.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * BUG FIX: AuthController and SportsAuctionController used
 * `com.sun.security.auth.UserPrincipal` — an internal JDK class that:
 *   (a) is not part of the public API,
 *   (b) does not have a getId() method returning Long,
 *   (c) will cause ClassCastException at runtime from Spring Security.
 *
 * Replaced with a proper app-level UserPrincipal that implements UserDetails
 * and carries the authenticated user's database ID.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final AppUser user;
    private final Long activeCommunityId;
    private final Long activeTowerId;
    private final Long activeFlatId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, authorities, null, null, null, null);
    }

    public UserPrincipal(Long id, String email, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         AppUser user) {
        this(id, email, password, authorities, user, null, null, null);
    }

    public UserPrincipal(Long id, String email, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         AppUser user,
                         Long activeCommunityId) {
        this(id, email, password, authorities, user, activeCommunityId, null, null);
    }

    public UserPrincipal(Long id, String email, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         AppUser user,
                         Long activeCommunityId,
                         Long activeTowerId,
                         Long activeFlatId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.user = user;
        this.activeCommunityId = activeCommunityId;
        this.activeTowerId = activeTowerId;
        this.activeFlatId = activeFlatId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return id; }
    public AppUser getUser() { return user; }

    public Long getCommunityId() {
        if (activeCommunityId != null) {
            return activeCommunityId;
        }
        return user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
    }

    public Long getTowerId() { return activeTowerId; }
    public Long getFlatId() { return activeFlatId; }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
