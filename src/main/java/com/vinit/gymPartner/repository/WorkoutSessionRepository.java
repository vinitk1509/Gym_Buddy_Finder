package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    List<WorkoutSession> findByMatch(Match match);

    @Query("""
    SELECT s FROM WorkoutSession s
    WHERE s.match.id = :matchId
    AND s.startDateTime < :end
    AND s.endDateTime > :start
    """)
    List<WorkoutSession> findOverlappingSessions(
            Long matchId,
            LocalDateTime start,
            LocalDateTime end
    );
    boolean existsByMatchAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Match match,
            LocalDateTime end,
            LocalDateTime start
    );
}
