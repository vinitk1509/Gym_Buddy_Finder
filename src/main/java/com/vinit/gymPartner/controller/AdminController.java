package com.vinit.gymPartner.controller;
import com.vinit.gymPartner.dto.AdminDashboardDTO;
import com.vinit.gymPartner.entity.enums.ReportStatus;
import com.vinit.gymPartner.service.AdminService;
import com.vinit.gymPartner.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            @RequestParam ReportStatus resolution,
            @RequestParam(required = false) String message,
            Authentication authentication) {
        Long adminId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        adminService.resolveReport(reportId, resolution, adminId, message);
        return ResponseEntity.ok("Report resolved");
    }

    @PostMapping("/users/{userId}/chat")
    public ResponseEntity<Map<String, Long>> openAdminChat(
            @PathVariable Long userId,
            Authentication authentication) {
        Long adminId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        Long matchId = adminService.openAdminConversation(adminId, userId);
        return ResponseEntity.ok(Map.of("matchId", matchId));
    }
}
