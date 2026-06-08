package com.pos.pos_backend.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long    userId;
    private final Long    tenantId;
    private final String  email;
    private final String  username;    // added — used in /me response
    private final String  shopName;    // added — used in /me response
    private final String  password;
    private final String  role;
    private final boolean active;
    private final String  schemaName;

    // Spring Security reads this to check what the user is allowed to do.
    // We prefix with ROLE_ because Spring Security expects that convention.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    // Spring Security uses getUsername() as the principal identifier.
    // We store email there since that's our login field.
    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    // Maps directly to our is_active column in the users table.
    // If admin deactivates a user, Spring Security blocks them automatically.
    @Override
    public boolean isEnabled() {
        return active;
    }
}