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
public class WorkoutSessionResponseDTO {
    private Long id;
    private MatchResponseDTO match;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String state;
    private Boolean requesterConfirmed;
    private Boolean receiverConfirmed;
    private Long createdById;    // who proposed the session
    private String createdByName;
    private Long requesterId;
    private String requesterName;
    private Long receiverId;
    private String receiverName;
}
