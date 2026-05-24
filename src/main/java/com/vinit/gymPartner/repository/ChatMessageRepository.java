package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByMatchIdOrderBySentAtAsc(Long matchId);
    Optional<ChatMessage> findTopByMatchIdOrderBySentAtDesc(Long matchId);
    long countByReceiverIdAndReadFalse(Long receiverId);
    long countByReceiverIdAndReadFalseAndMatchId(Long receiverId, Long matchId);

    @Modifying
    @Query("""
            UPDATE ChatMessage c
            SET c.read = true
            WHERE c.matchId = :matchId
              AND c.receiverId = :userId
              AND c.read = false
            """)
    int markUnreadMessagesRead(Long matchId, Long userId);
}
