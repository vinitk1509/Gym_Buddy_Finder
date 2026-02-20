package com.vinit.gymPartner.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MatchResultDTO {

    private Long userId;
    private String fullName;
    private String experienceLevel;
    private String goal;
    private String workoutType;
    private double compatibilityScore;
}