package com.vinit.gymPartner.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private Long matchId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String sentAt;
}
