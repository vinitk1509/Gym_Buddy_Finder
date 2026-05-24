package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserReport;
import com.vinit.gymPartner.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport,Long> {
    boolean existsByReporterAndReportedUser(User reporter, User reportedUser);

    List<UserReport> findByStatus(ReportStatus status);
    long countByStatus(ReportStatus status);
    List<UserReport> findTop10ByOrderByCreatedAtDesc();
    long countByReportedUserAndStatus(User user, ReportStatus reportStatus);
    List<UserReport> findByReporterOrderByCreatedAtDesc(User reporter);
}
