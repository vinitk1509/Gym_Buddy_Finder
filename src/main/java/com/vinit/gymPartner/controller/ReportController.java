package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.ReportRequestDTO;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

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

}
