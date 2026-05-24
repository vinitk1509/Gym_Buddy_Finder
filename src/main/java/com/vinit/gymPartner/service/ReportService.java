package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.MyReportDTO;
import com.vinit.gymPartner.dto.ReportRequestDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserReport;
import com.vinit.gymPartner.entity.enums.ReportStatus;
import com.vinit.gymPartner.entity.enums.UserStatus;
import com.vinit.gymPartner.repository.UserReportRepository;
import com.vinit.gymPartner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final UserRepository userRepository;
    private final UserReportRepository reportRepository;
    private final UserService userService;

    public void reportUser(Long reporterId, Long reportedId, ReportRequestDTO dto)
    {
        if (reporterId.equals(reportedId))
            throw new RuntimeException("You cannot report yourself.");

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        User reportedUser = userRepository.findById(reportedId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (reportedUser.getStatus() != UserStatus.ACTIVE)
            throw new RuntimeException("Cannot report inactive user.");

        boolean alreadyReported =
                reportRepository.existsByReporterAndReportedUser(
                        reporter, reportedUser);

        if (alreadyReported)
            throw new RuntimeException("You have already reported this user.");


        UserReport report = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        reportRepository.save(report);
        userService.updateReliability(reportedUser, -5);
        long openReports = reportRepository.countByReportedUserAndStatus(
                reportedUser, ReportStatus.OPEN
        );

        if (openReports >= 3) {
            userService.updateReliability(reportedUser, -15);
        }
    }

    public List<MyReportDTO> getMyReports(Long reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        return reportRepository.findByReporterOrderByCreatedAtDesc(reporter)
                .stream()
                .map(report -> MyReportDTO.builder()
                        .reportId(report.getId())
                        .reportedUserId(report.getReportedUser().getId())
                        .reportedUserName(report.getReportedUser().getName())
                        .reason(report.getReason().name())
                        .description(report.getDescription())
                        .status(report.getStatus().name())
                        .createdAt(report.getCreatedAt())
                        .build())
                .toList();
    }

}
