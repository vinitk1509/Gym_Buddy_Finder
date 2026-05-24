package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.GroupWorkoutSession;
import com.vinit.gymPartner.entity.enums.SessionState;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupWorkoutSessionRepository extends JpaRepository<GroupWorkoutSession, Long> {
    @Query("""
    SELECT s FROM GroupWorkoutSession s
    JOIN s.group g
    JOIN g.members m
    WHERE m.id = :userId
    ORDER BY s.startDateTime DESC
    """)
    List<GroupWorkoutSession> findByMemberId(@Param("userId") Long userId);

    @Query("""
    SELECT s FROM GroupWorkoutSession s
    WHERE s.group.id = :groupId
    AND s.startDateTime < :end
    AND s.endDateTime > :start
    AND s.state IN :states
    """)
    List<GroupWorkoutSession> findOverlappingSessions(
            @Param("groupId") Long groupId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("states") List<SessionState> states
    );

    @Query("""
    SELECT s FROM GroupWorkoutSession s
    JOIN s.group g
    JOIN g.members m
    WHERE m.id = :userId
    AND s.startDateTime < :end
    AND s.endDateTime > :start
    AND s.state IN :states
    """)
    List<GroupWorkoutSession> findOverlappingSessionsForUser(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("states") List<SessionState> states
    );

    void deleteByGroupId(Long groupId);
}
