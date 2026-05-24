package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.Gym;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    @Query("""
    SELECT m FROM Match m
    WHERE (m.requester.id = :userId1 AND m.receiver.id = :userId2)
       OR (m.requester.id = :userId2 AND m.receiver.id = :userId1)
    ORDER BY
        CASE
            WHEN m.status = com.vinit.gymPartner.entity.enums.MatchStatus.ACCEPTED THEN 0
            WHEN m.status = com.vinit.gymPartner.entity.enums.MatchStatus.PENDING THEN 1
            ELSE 2
        END,
        m.updatedAt DESC,
        m.id DESC
    """)
    List<Match> findMatchesBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    // active match check
    boolean existsByRequesterIdAndReceiverIdAndStatus(
            Long requesterId,
            Long receiverId,
            MatchStatus status
    );

    List<Match> findByRequesterIdOrReceiverId(Long requesterId, Long receiverId);
    List<Match> findByRequesterIdAndStatus(Long requesterId, MatchStatus status);
    List<Match> findByReceiverIdAndStatus(Long receiverId, MatchStatus status);

    List<Match> findByStatusAndRequesterIdOrStatusAndReceiverId(
            MatchStatus status1, Long requesterId,
            MatchStatus status2, Long receiverId
    );

    void deleteByRequesterAndReceiver(User requester, User receiver);

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM Match m
    WHERE (m.requester = :user1 AND m.receiver = :user2)
       OR (m.requester = :user2 AND m.receiver = :user1)
    """)
    void deleteMatchBetweenUsers(@Param("user1") User user1,
                                 @Param("user2") User user2);

    boolean existsByRequesterAndReceiver(User requester, User receiver);

    boolean existsByRequesterAndReceiverAndStatus(
            User requester,
            User receiver,
            MatchStatus status
    );

    /**
     * Check if a user (as requester OR receiver) has any match with the given status.
     * Uses explicit @Query because Spring Data derived queries parse OR/AND with wrong precedence.
     */
    @Query("""
    SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
    FROM Match m
    WHERE (m.requester = :user OR m.receiver = :user)
    AND m.status = :status
    """)
    boolean existsByUserAndStatus(
            @Param("user") User user,
            @Param("status") MatchStatus status
    );

    @Query("""
    SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
    FROM Match m
    WHERE (m.requester = :user OR m.receiver = :user)
    AND m.status = :status
    AND m.gym = :gym
    """)
    boolean existsByUserAndStatusAndGym(
            @Param("user") User user,
            @Param("status") MatchStatus status,
            @Param("gym") Gym gym
    );

    /**
     * Find all matches where the user is requester OR receiver with a given status.
     * Uses explicit @Query because Spring Data derived queries parse OR/AND with wrong precedence.
     */
    @Query("""
    SELECT m FROM Match m
    WHERE (m.requester = :user OR m.receiver = :user)
    AND m.status = :status
    """)
    List<Match> findAllByUserAndStatus(
            @Param("user") User user,
            @Param("status") MatchStatus status
    );

    List<Match> findByRequesterAndStatus(
            User requester,
            MatchStatus status
    );

    List<Match> findByReceiverAndStatus(
            User receiver,
            MatchStatus status
    );

    List<Match> findByRequesterOrReceiver(
            User requester,
            User receiver
    );

    @Modifying
    @Query("""
    UPDATE Match m
    SET m.status = 'EXPIRED'
    WHERE m.status = 'PENDING'
    AND m.expiresAt <= :now
    """)
    int expireOldMatches(@Param("now") LocalDateTime now);

    @Query("""
    SELECT COUNT(m)
    FROM Match m
    WHERE m.requester.id = :userId
    AND m.createdAt >= :startOfDay
    """)
    long countTodayRequests(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    List<Match> findByStatusAndExpiresAtBefore(MatchStatus status, LocalDateTime now);
    List<Match> findTop10ByOrderByUpdatedAtDesc();

    @Query("""
    SELECT m FROM Match m
    WHERE (m.requester = :user OR m.receiver = :user)
    AND m.gym = :gym
    AND m.status = com.vinit.gymPartner.entity.enums.MatchStatus.PENDING
    """)
    List<Match> findPendingByUserAndGym(
            @Param("user") User user,
            @Param("gym") com.vinit.gymPartner.entity.Gym gym
    );
}
