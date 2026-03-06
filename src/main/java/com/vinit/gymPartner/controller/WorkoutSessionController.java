package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.entity.WorkoutSession;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.WorkoutSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    // CREATE SESSION
    @PostMapping("/create")
    public ResponseEntity<WorkoutSession> createSession(
            @RequestParam Long matchId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        WorkoutSession session = workoutSessionService.createSession(
                matchId,
                userDetails.getUserId(),
                start,
                end
        );

        return ResponseEntity.ok(session);
    }

    // CONFIRM ATTENDANCE
    @PostMapping("/{sessionId}/confirm")
    public ResponseEntity<String> confirmAttendance(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        workoutSessionService.confirmAttendance(sessionId, userDetails.getUserId());

        return ResponseEntity.ok("Attendance confirmed");
    }

    // REPORT NO SHOW
    @PostMapping("/{sessionId}/no-show")
    public ResponseEntity<String> reportNoShow(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        workoutSessionService.reportNoShow(sessionId, userDetails.getUserId());

        return ResponseEntity.ok("No-show reported");
    }

    // CANCEL SESSION
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<String> cancelSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        workoutSessionService.cancelSession(sessionId, userDetails.getUserId());

        return ResponseEntity.ok("Session cancelled");
    }
}