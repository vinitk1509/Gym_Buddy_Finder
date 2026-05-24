package com.vinit.gymPartner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymGroupDTO {
    private Long id;
    private String name;
    private String description;
    private Long creatorId;
    private String creatorName;
    private String gymName;
    private Integer targetCapacity;
    private Integer memberCount;
    private Boolean currentUserMember;
    private Boolean currentUserCreator;
    private Boolean banned;
    private Long bannedById;
    private String bannedByName;
    private LocalDateTime bannedAt;
    private LocalDateTime createdAt;
    private List<MemberDTO> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDTO {
        private Long userId;
        private String name;
        private String profilePictureUrl;
        private Integer reliabilityScore;
        private Integer age;
        private String gymName;
        private String fitnessGoal;
        private String workoutType;
        private String experienceLevel;
        private String bio;
        private Integer targetGroupSize;
    }
}
