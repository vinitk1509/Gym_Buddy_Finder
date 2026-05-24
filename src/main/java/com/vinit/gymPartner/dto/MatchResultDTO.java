package com.vinit.gymPartner.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchResultDTO {

    private Long userId;
    private String fullName;
    private int age;
    private String experienceLevel;
    private String goal;
    private String workoutType;
    private double compatibilityScore;
    private String profilePictureUrl;
    private String gymName;
    private String bio;
    private Integer reliabilityScore;
    private Long matchId;
    private String relationshipStatus;
    private String relationshipDirection;
    private Boolean canSendRequest;
}
