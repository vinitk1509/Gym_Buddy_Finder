package com.vinit.gymPartner.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fitnessGoal;
    private String experienceLevel;
    private boolean lookingForPartner;
}
