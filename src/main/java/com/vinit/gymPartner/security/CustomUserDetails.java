package com.vinit.gymPartner.security;

import com.vinit.gymPartner.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import com.vinit.gymPartner.entity.enums.UserRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;


@RequiredArgsConstructor
@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    @Getter
    private Long userId;
    private String email;
    private String password;


    private UserRole role;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return Collections.emptyList();
        }
        // Spring Security's hasRole() expects the "ROLE_" prefix by default
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}