package com.vinit.gymPartner.dto;

import com.vinit.gymPartner.entity.enums.UserRole;
import com.vinit.gymPartner.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String gymName;
    private String gymAddress;
    private UserRole role;
    private UserStatus status;
    private Integer reliabilityScore;
    private Boolean allowMultiplePartners;
    private Boolean lookingForPartner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User's fitness profile info
    private String experienceLevel;
    private String fitnessGoal;
    private String workoutType;
}
