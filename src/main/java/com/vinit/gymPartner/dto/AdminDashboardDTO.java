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
    private List<UserReportSummaryDTO> recentReports;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class UserReportSummaryDTO {
        private Long reportId;
        private String reporterName;
        private String reportedUserName;
        private String reason;
        private String status;
        private String createdAt;
    }
}