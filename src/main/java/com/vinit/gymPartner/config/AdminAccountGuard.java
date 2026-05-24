package com.vinit.gymPartner.config;

import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccountGuard implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        if (adminCount > 1) {
            throw new IllegalStateException("Only one admin account is allowed. Found " + adminCount + " admin users.");
        }
    }
}
