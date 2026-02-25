package com.vinit.gymPartner.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class MatchResponseDTO {
    private Long id;
    private Long requesterId;
    private String requesterEmail;

    private Long receiverId;
    private String receiverEmail;

    private String status;
    private Double compatibilityScore;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime terminatedAt;
}
