package com.vinit.gymPartner.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdminDashboardDTO {

    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long totalMatches;
    private long openReports;
    private List<AdminUserSummaryDTO> users;
    private List<UserReportSummaryDTO> recentReports;
    private List<AdminMatchSummaryDTO> recentMatches;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class AdminUserSummaryDTO {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private String status;
        private Integer reliabilityScore;
        private Integer age;
        private String gender;
        private String profilePictureUrl;
        private String bio;
        private String gymName;
        private Boolean lookingForPartner;
        private Boolean allowMultiplePartners;
        private Integer targetGroupSize;
        private String fitnessGoal;
        private String workoutType;
        private String experienceLevel;
        private String preferredPartnerGender;
        private Integer preferredMinAge;
        private Integer preferredMaxAge;
        private String joinedAt;
        private String lastLoginAt;
        private String lastSeenAt;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class UserReportSummaryDTO {
        private Long reportId;
        private Long reporterId;
        private String reporterName;
        private Long reportedUserId;
        private String reportedUserName;
        private String reason;
        private String description;
        private String status;
        private String createdAt;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class AdminMatchSummaryDTO {
        private Long matchId;
        private String requesterName;
        private String receiverName;
        private String gymName;
        private String status;
        private Double compatibilityScore;
        private String createdAt;
        private String updatedAt;
    }
}
