package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.MyReportDTO;
import com.vinit.gymPartner.dto.ReportRequestDTO;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/{id}/report")
    public ResponseEntity<String> reportUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReportRequestDTO dto
            ){
        reportService.reportUser(
                userDetails.getUserId(),
                id,
                dto
        );
        return ResponseEntity.ok("User Reported Successfully");
    }

    @GetMapping("/reports")
    public ResponseEntity<List<MyReportDTO>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(reportService.getMyReports(userDetails.getUserId()));
    }

}
