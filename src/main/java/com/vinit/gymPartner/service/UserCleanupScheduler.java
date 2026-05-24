package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily scheduled job that handles soft-delete lifecycle:
 * 1. Users who requested deletion 30+ days ago → permanently deleted.
 * 2. Users who haven't logged in for 30+ days → marked INACTIVE (auto soft-delete).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    private static final int DELETION_GRACE_DAYS = 30;
    private static final int INACTIVITY_DAYS = 30;

    /**
     * Runs every day at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupUsers() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Permanently delete users whose deletion was requested 30+ days ago
        LocalDateTime deletionCutoff = now.minusDays(DELETION_GRACE_DAYS);
        List<User> toDelete = userRepository.findByStatusAndDeletionRequestedAtBefore(
                UserStatus.INACTIVE, deletionCutoff);

        if (!toDelete.isEmpty()) {
            log.info("Permanently deleting {} users whose deletion was requested 30+ days ago", toDelete.size());
            userRepository.deleteAll(toDelete);
        }

        // 2. Mark users inactive if they haven't logged in for 30+ days
        LocalDateTime inactivityCutoff = now.minusDays(INACTIVITY_DAYS);
        List<User> inactiveUsers = userRepository.findByStatusNotAndLastLoginAtBefore(
                UserStatus.INACTIVE, inactivityCutoff);

        for (User user : inactiveUsers) {
            if (user.getStatus() == UserStatus.BANNED) continue; // Don't auto-touch banned users
            log.info("Marking user {} ({}) as INACTIVE due to {} days of inactivity",
                    user.getId(), user.getName(), INACTIVITY_DAYS);
            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
        }
    }
}
