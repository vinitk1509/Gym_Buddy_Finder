package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.AvailabilitySlotDTO;
import com.vinit.gymPartner.dto.MatchResponseDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final com.vinit.gymPartner.service.WorkoutSessionService workoutSessionService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    public ResponseEntity<MatchResponseDTO> sendRequest(
            @RequestParam Long receiverId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User requester = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found"));

        MatchResponseDTO match = matchService.sendMatchRequest(requester.getId(), receiverId);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/{matchId}/accept")
    public ResponseEntity<MatchResponseDTO> acceptMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        MatchResponseDTO updatedMatch = matchService.acceptmatch(matchId, email);
        return ResponseEntity.ok(updatedMatch);
    }

    @PostMapping("/{matchId}/reject")
    public ResponseEntity<MatchResponseDTO> rejectMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        MatchResponseDTO result = matchService.rejectMatch(matchId, authentication);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{matchId}/cancel")
    public ResponseEntity<MatchResponseDTO> cancelMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        MatchResponseDTO result = matchService.cancelMatch(matchId, authentication);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{matchId}/terminate")
    public ResponseEntity<MatchResponseDTO> terminateMatch(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        MatchResponseDTO result = matchService.terminateMatch(matchId, authentication);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{matchId}/mutual-availability")
    public ResponseEntity<List<AvailabilitySlotDTO>> getMutualAvailability(
            @PathVariable Long matchId,
            Authentication authentication
    ) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(workoutSessionService.getMutualAvailability(matchId, user.getId()));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<MatchResponseDTO>> getMySentRequests(
            Authentication authentication
    ) {
        return ResponseEntity.ok(matchService.getMySentRequests(authentication));
    }

    @GetMapping("/received")
    public ResponseEntity<List<MatchResponseDTO>> getMyReceivedRequests(
            Authentication authentication
    ) {
        return ResponseEntity.ok(matchService.getMyReceivedRequests(authentication));
    }

    @GetMapping("/active")
    public ResponseEntity<List<MatchResponseDTO>> getMyActiveMatches(
            Authentication authentication
    ) {
        return ResponseEntity.ok(matchService.getMyActiveMatches(authentication));
    }

    @GetMapping("/history")
    public ResponseEntity<List<MatchResponseDTO>> getMyMatchHistory(
            Authentication authentication
    ) {
        return ResponseEntity.ok(matchService.getMyMatchHistory(authentication));
    }
}
