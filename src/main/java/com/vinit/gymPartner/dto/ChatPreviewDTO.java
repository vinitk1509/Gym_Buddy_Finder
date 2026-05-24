package com.vinit.gymPartner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatPreviewDTO {
    private Long matchId;
    private Long partnerId;
    private String partnerName;
    private String partnerProfilePicture;
    private boolean partnerActiveNow;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
