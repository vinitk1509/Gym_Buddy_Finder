package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
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
    Optional<Match> findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(
            Long requesterId1,
            Long receiverId1,
            Long requesterId2,
            Long receiverId2
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

    boolean existsByRequesterOrReceiverAndStatus(
            User requester,
            User receiver,
            MatchStatus status
    );

    List<Match> findAllByRequesterOrReceiverAndStatus(
            User requester,
            User receiver,
            MatchStatus status
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


}
