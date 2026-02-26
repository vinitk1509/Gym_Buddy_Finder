package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.DashboardResponseDTO;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        return ResponseEntity.ok(
                dashboardService.buildDashboard(userDetails.getUserId())
        );
    }
}
