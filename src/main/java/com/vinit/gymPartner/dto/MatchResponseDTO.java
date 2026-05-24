package com.vinit.gymPartner.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class MatchResponseDTO {
    private Long id;
    private Long requesterId;
    private String requesterEmail;
    private String requesterName;
    private String requesterProfilePicture;
    private String requesterGymName;
    private Integer requesterAge;
    private String requesterFitnessGoal;
    private String requesterWorkoutType;
    private String requesterExperienceLevel;
    private Integer requesterReliabilityScore;
    private Boolean requesterActiveNow;
    private Integer requesterTargetGroupSize;

    private Long receiverId;
    private String receiverEmail;
    private String receiverName;
    private String receiverProfilePicture;
    private String receiverGymName;
    private Integer receiverAge;
    private String receiverFitnessGoal;
    private String receiverWorkoutType;
    private String receiverExperienceLevel;
    private Integer receiverReliabilityScore;
    private Boolean receiverActiveNow;
    private Integer receiverTargetGroupSize;

    private String status;
    private Double compatibilityScore;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime terminatedAt;

    // Who terminated the match
    private Long terminatedById;
    private String terminatedByName;
}
