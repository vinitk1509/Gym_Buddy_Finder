package com.vinit.gymPartner.service;
import com.vinit.gymPartner.dto.AdminDashboardDTO;
import com.vinit.gymPartner.dto.ChatMessageDTO;
import com.vinit.gymPartner.entity.FitnessProfile;
import com.vinit.gymPartner.entity.Match;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserReport;
import com.vinit.gymPartner.entity.enums.*;
import com.vinit.gymPartner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserReportRepository userReportRepository;
    private final ChatService chatService;

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
                .users(getUsers())
                .recentReports(getRecentReports())
                .recentMatches(getRecentMatches())
                .build();
    }


    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);

    }


    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

    }


    @Transactional
    public void resolveReport(Long reportId, ReportStatus resolution, Long adminId, String message) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        if (message != null && !message.isBlank()) {
            Long matchId = openAdminConversation(adminId, report.getReporter().getId());
            chatService.sendMessage(ChatMessageDTO.builder()
                    .matchId(matchId)
                    .senderId(adminId)
                    .receiverId(report.getReporter().getId())
                    .content(message.trim())
                    .build());
        }
        report.setStatus(resolution);
        userReportRepository.save(report);

    }

    @Transactional
    public Long openAdminConversation(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new RuntimeException("Admin cannot open a support chat with themselves");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Only admins can open support chats");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Admin support chats must target a regular user");
        }

        Match existingMatch = matchRepository.findMatchesBetweenUsers(adminId, userId)
                .stream()
                .filter(match -> match.getStatus() == MatchStatus.ACCEPTED)
                .findFirst()
                .orElse(null);
        if (existingMatch != null) {
            return existingMatch.getId();
        }

        Match supportMatch = Match.builder()
                .gym(user.getGym())
                .requester(admin)
                .receiver(user)
                .status(MatchStatus.ACCEPTED)
                .compatibilityScore(0.0)
                .expiresAt(LocalDateTime.now().plusYears(10))
                .build();

        return matchRepository.save(supportMatch).getId();
    }

    /**
     * Get the 10 most recent reports for the dashboard quick view.
     */
    private List<AdminDashboardDTO.AdminUserSummaryDTO> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::buildAdminUserSummary)
                .toList();
    }

    private AdminDashboardDTO.AdminUserSummaryDTO buildAdminUserSummary(User user) {
        FitnessProfile profile = user.getFitnessProfile();

        return AdminDashboardDTO.AdminUserSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .reliabilityScore(user.getReliabilityScore())
                .age(user.getAge())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(user.getBio())
                .gymName(user.getGym() != null ? user.getGym().getName() : null)
                .lookingForPartner(user.isLookingForPartner())
                .allowMultiplePartners(user.getAllowMultiplePartners())
                .targetGroupSize(user.getTargetGroupSize())
                .fitnessGoal(profile != null && profile.getGoal() != null ? profile.getGoal().name() : null)
                .workoutType(profile != null && profile.getWorkoutType() != null ? profile.getWorkoutType().name() : null)
                .experienceLevel(profile != null && profile.getExperienceLevel() != null ? profile.getExperienceLevel().name() : null)
                .preferredPartnerGender(profile != null && profile.getPreferredPartnerGender() != null ? profile.getPreferredPartnerGender().name() : null)
                .preferredMinAge(profile != null ? profile.getPreferredMinAge() : null)
                .preferredMaxAge(profile != null ? profile.getPreferredMaxAge() : null)
                .joinedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .lastSeenAt(user.getLastSeenAt() != null ? user.getLastSeenAt().toString() : null)
                .build();
    }

    /**
     * Get the 10 most recent reports for the dashboard quick view.
     */
    private List<AdminDashboardDTO.UserReportSummaryDTO> getRecentReports() {
        return userReportRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(r -> AdminDashboardDTO.UserReportSummaryDTO.builder()
                        .reportId(r.getId())
                        .reporterId(r.getReporter().getId())
                        .reporterName(r.getReporter().getName())
                        .reportedUserId(r.getReportedUser().getId())
                        .reportedUserName(r.getReportedUser().getName())
                        .reason(r.getReason().name())
                        .description(r.getDescription())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "N/A")
                        .build())
                .toList();
    }

    private List<AdminDashboardDTO.AdminMatchSummaryDTO> getRecentMatches() {
        return matchRepository.findTop10ByOrderByUpdatedAtDesc()
                .stream()
                .map(match -> AdminDashboardDTO.AdminMatchSummaryDTO.builder()
                        .matchId(match.getId())
                        .requesterName(match.getRequester() != null ? ChatService.displayName(match.getRequester()) : null)
                        .receiverName(match.getReceiver() != null ? ChatService.displayName(match.getReceiver()) : null)
                        .gymName(match.getGym() != null ? match.getGym().getName() : null)
                        .status(match.getStatus() != null ? match.getStatus().name() : null)
                        .compatibilityScore(match.getCompatibilityScore())
                        .createdAt(match.getCreatedAt() != null ? match.getCreatedAt().toString() : null)
                        .updatedAt(match.getUpdatedAt() != null ? match.getUpdatedAt().toString() : null)
                        .build())
                .toList();
    }
}
