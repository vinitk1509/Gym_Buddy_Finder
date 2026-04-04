package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByMatchIdOrderBySentAtAsc(Long matchId);
    long countByReceiverIdAndReadFalse(Long receiverId);
}
