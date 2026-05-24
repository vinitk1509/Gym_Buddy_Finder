package com.vinit.gymPartner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_email_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_email_match_sender_receiver",
                columnNames = {"match_id", "sender_id", "receiver_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(nullable = false)
    private LocalDateTime lastSentAt;
}
