package com.vinit.gymPartner.security;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCaseAndStatus(normalizedEmail, UserStatus.ACTIVE)
                .orElseThrow(()->
                        new UsernameNotFoundException("User Not Found oe inactive"));
        return new CustomUserDetails(user);
    }
}
