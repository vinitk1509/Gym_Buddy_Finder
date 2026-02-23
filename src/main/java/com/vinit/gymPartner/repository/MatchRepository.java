package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
