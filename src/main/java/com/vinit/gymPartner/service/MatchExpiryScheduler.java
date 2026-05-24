package com.vinit.gymPartner.service;

import com.vinit.gymPartner.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchExpiryScheduler {

    private final MatchRepository matchRepository;
    @Scheduled(cron = "0 0 * * * ?")
    // runs every hour
    @Transactional
    public void expirePendingMatches() {

        int expiredCount =
                matchRepository.expireOldMatches(LocalDateTime.now());

        if (expiredCount > 0) {
            log.info("{} matches expired.", expiredCount);
        }
    }
}
