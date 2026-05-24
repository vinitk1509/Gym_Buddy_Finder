package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    List<WorkoutSession> findByMatch(Match match);

    void deleteByMatch(Match match);

    @Query("""
    SELECT s FROM WorkoutSession s
    WHERE s.match.requester.id = :userId
       OR s.match.receiver.id = :userId
    ORDER BY s.startDateTime DESC
    """)
    List<WorkoutSession> findByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT COUNT(s) FROM WorkoutSession s
    WHERE (s.match.requester.id = :userId OR s.match.receiver.id = :userId)
    AND s.state IN (
        com.vinit.gymPartner.entity.enums.SessionState.PENDING_APPROVAL,
        com.vinit.gymPartner.entity.enums.SessionState.SCHEDULED,
        com.vinit.gymPartner.entity.enums.SessionState.COMPLETED,
        com.vinit.gymPartner.entity.enums.SessionState.NO_SHOW
    )
    """)
    long countMeaningfulSessionsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT COUNT(s) FROM WorkoutSession s
    WHERE (s.match.requester.id = :userId OR s.match.receiver.id = :userId)
    AND s.state = com.vinit.gymPartner.entity.enums.SessionState.COMPLETED
    """)
    long countCompletedSessionsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT s FROM WorkoutSession s
    WHERE (s.match.requester.id = :userId OR s.match.receiver.id = :userId)
    AND s.state = com.vinit.gymPartner.entity.enums.SessionState.COMPLETED
    ORDER BY s.startDateTime DESC
    """)
    List<WorkoutSession> findCompletedSessionsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT s FROM WorkoutSession s
    WHERE s.match.id = :matchId
    AND s.startDateTime < :end
    AND s.endDateTime > :start
    AND s.state IN (
        com.vinit.gymPartner.entity.enums.SessionState.PENDING_APPROVAL,
        com.vinit.gymPartner.entity.enums.SessionState.SCHEDULED
    )
    """)
    List<WorkoutSession> findOverlappingSessions(
            Long matchId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
    SELECT s FROM WorkoutSession s
    WHERE (s.match.requester.id = :userId OR s.match.receiver.id = :userId)
    AND s.startDateTime < :end
    AND s.endDateTime > :start
    AND s.state IN (
        com.vinit.gymPartner.entity.enums.SessionState.PENDING_APPROVAL,
        com.vinit.gymPartner.entity.enums.SessionState.SCHEDULED
    )
    """)
    List<WorkoutSession> findOverlappingSessionsForUser(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    boolean existsByMatchAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            Match match,
            LocalDateTime end,
            LocalDateTime start
    );
}
