package com.vinit.gymPartner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatMessageDTO {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String senderProfilePictureUrl;
    private String content;
    private LocalDateTime sentAt;
}
