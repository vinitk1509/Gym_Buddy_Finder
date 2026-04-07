package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import com.vinit.gymPartner.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchExpiryScheduler {

    private final MatchRepository matchRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * ?")
    // runs every hour
    @Transactional
    public void expirePendingMatches() {

        // Find matches that are about to expire so we can notify users
        List<Match> expiringMatches = matchRepository.findByStatusAndExpiresAtBefore(
                MatchStatus.PENDING, LocalDateTime.now());

        // Notify both users for each expiring match
        for (Match match : expiringMatches) {
            notificationService.sendToUser(
                    match.getRequester().getId(),
                    "Match Expired ⏰",
                    "Your match request to " + match.getReceiver().getName() + " has expired."
            );
            notificationService.sendToUser(
                    match.getReceiver().getId(),
                    "Match Expired ⏰",
                    "A match request from " + match.getRequester().getName() + " has expired."
            );
        }

        // Now expire them in bulk
        int expiredCount =
                matchRepository.expireOldMatches(LocalDateTime.now());

        if (expiredCount > 0) {
            log.info("{} matches expired and users notified.", expiredCount);
        }
    }
}