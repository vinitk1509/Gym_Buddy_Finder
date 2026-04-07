package com.vinit.gymPartner.service;
import com.vinit.gymPartner.dto.AdminDashboardDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserReport;
import com.vinit.gymPartner.entity.enums.*;
import com.vinit.gymPartner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserReportRepository userReportRepository;
    private final NotificationService notificationService;

    /**
     * Build the admin dashboard with platform-wide statistics.
     */
    public AdminDashboardDTO getDashboard() {
        return AdminDashboardDTO.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .bannedUsers(userRepository.countByStatus(UserStatus.BANNED))
                .totalMatches(matchRepository.count())
                .openReports(userReportRepository.countByStatus(ReportStatus.OPEN))
                .recentReports(getRecentReports())
                .build();
    }


    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);

        // Notify the banned user
        notificationService.sendToUser(
                userId,
                "Account Suspended",
                "Your account has been suspended due to violations of community guidelines. Contact support for more information."
        );
    }


    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Notify the unbanned user
        notificationService.sendToUser(
                userId,
                "Account Restored ✅",
                "Your account has been reactivated. Welcome back to GymPartner!"
        );
    }


    public void resolveReport(Long reportId, ReportStatus resolution) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(resolution);
        userReportRepository.save(report);

        // Notify the reporter that their report has been resolved
        notificationService.sendToUser(
                report.getReporter().getId(),
                "Report Update",
                "Your report has been reviewed and marked as: " + resolution.name() + ". Thank you for helping keep the community safe."
        );
    }

    /**
     * Get the 10 most recent reports for the dashboard quick view.
     */
    private List<AdminDashboardDTO.UserReportSummaryDTO> getRecentReports() {
        return userReportRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(r -> AdminDashboardDTO.UserReportSummaryDTO.builder()
                        .reportId(r.getId())
                        .reporterName(r.getReporter().getName())
                        .reportedUserName(r.getReportedUser().getName())
                        .reason(r.getReason().name())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "N/A")
                        .build())
                .toList();
    }
}