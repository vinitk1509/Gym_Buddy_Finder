package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.ChatEmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatEmailNotificationRepository extends JpaRepository<ChatEmailNotification, Long> {

    Optional<ChatEmailNotification> findByMatchIdAndSenderIdAndReceiverId(
            Long matchId,
            Long senderId,
            Long receiverId
    );
}
