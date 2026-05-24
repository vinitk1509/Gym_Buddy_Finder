package com.vinit.gymPartner.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fitnessGoal;
    private String experienceLevel;
    private String workoutType;
    private Boolean lookingForPartner;
    private Boolean allowMultiplePartners;
    private Integer targetGroupSize;
    private String bio;
    private String name;
    private String gymName;
    private String gymAddress;
    private Double latitude;
    private Double longitude;
    private String placeId;
}
