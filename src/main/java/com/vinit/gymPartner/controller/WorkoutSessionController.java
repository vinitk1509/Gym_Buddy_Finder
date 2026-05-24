package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.entity.WorkoutSession;
import com.vinit.gymPartner.security.CustomUserDetails;
import com.vinit.gymPartner.service.WorkoutSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.vinit.gymPartner.dto.CreateSessionRequestDTO;
import com.vinit.gymPartner.dto.WorkoutSessionResponseDTO;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    // GET MY SESSIONS
    @GetMapping("/my")
    public ResponseEntity<java.util.List<WorkoutSessionResponseDTO>> getMySessions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                workoutSessionService.getSessionsByUser(userDetails.getUserId())
                        .stream()
                        .map(this::mapToDTO)
                        .collect(Collectors.toList())
        );
    }

    // CREATE SESSION
    @PostMapping("/create")
    public ResponseEntity<WorkoutSessionResponseDTO> createSession(
            @RequestBody CreateSessionRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        WorkoutSession session = workoutSessionService.createSession(
                requestDTO.getMatchId(),
                userDetails.getUserId(),
                requestDTO.getStart(),
                requestDTO.getEnd()
        );

        return ResponseEntity.ok(mapToDTO(session));
    }

    private WorkoutSessionResponseDTO mapToDTO(WorkoutSession session) {
        com.vinit.gymPartner.dto.MatchResponseDTO matchDTO = new com.vinit.gymPartner.dto.MatchResponseDTO();
        matchDTO.setId(session.getMatch().getId());
        matchDTO.setRequesterId(session.getMatch().getRequester().getId());
        matchDTO.setRequesterName(session.getMatch().getRequester().getName());
        matchDTO.setReceiverId(session.getMatch().getReceiver().getId());
        matchDTO.setReceiverName(session.getMatch().getReceiver().getName());

        return WorkoutSessionResponseDTO.builder()
                .id(session.getId())
                .match(matchDTO)
                .startDateTime(session.getStartDateTime())
                .endDateTime(session.getEndDateTime())
                .state(session.getState().name())
                .requesterConfirmed(session.getRequesterConfirmed())
                .receiverConfirmed(session.getReceiverConfirmed())
                .createdById(session.getCreatedBy() != null ? session.getCreatedBy().getId() : null)
                .createdByName(session.getCreatedBy() != null ? session.getCreatedBy().getName() : null)
                .requesterId(session.getMatch().getRequester().getId())
                .requesterName(session.getMatch().getRequester().getName())
                .receiverId(session.getMatch().getReceiver().getId())
                .receiverName(session.getMatch().getReceiver().getName())
                .build();
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

    // APPROVE SESSION (partner accepts the request)
    @PostMapping("/{sessionId}/approve")
    public ResponseEntity<String> approveSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        workoutSessionService.approveSession(sessionId, userDetails.getUserId());
        return ResponseEntity.ok("Session approved");
    }

    // DECLINE SESSION (partner declines the request)
    @PostMapping("/{sessionId}/decline")
    public ResponseEntity<String> declineSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        workoutSessionService.declineSession(sessionId, userDetails.getUserId());
        return ResponseEntity.ok("Session declined");
    }
}
