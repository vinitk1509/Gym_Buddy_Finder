package com.vinit.gymPartner.controller;
import com.vinit.gymPartner.dto.AdminDashboardDTO;
import com.vinit.gymPartner.entity.enums.ReportStatus;
import com.vinit.gymPartner.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;


    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }


    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<String> banUser(@PathVariable Long userId) {
        adminService.banUser(userId);
        return ResponseEntity.ok("User banned successfully");
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return ResponseEntity.ok("User unbanned successfully");
    }


    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<String> resolveReport(
            @PathVariable Long reportId,
            @RequestParam ReportStatus resolution) {
        adminService.resolveReport(reportId, resolution);
        return ResponseEntity.ok("Report resolved");
    }
}