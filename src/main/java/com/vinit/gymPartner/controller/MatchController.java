package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.MatchResponseDTO;
import com.vinit.gymPartner.entity.Match;
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

    @PutMapping("/reject/{matchId}")
    public ResponseEntity<MatchResponseDTO> reject(
            @PathVariable Long matchId, Authentication authentication){
        return ResponseEntity.ok(matchService.rejectMatch(matchId,authentication));
    }

    @PutMapping("/cancel/{matchId}")
    public ResponseEntity<MatchResponseDTO> cancel(@PathVariable Long matchId, Authentication authentication)
    {
        return ResponseEntity.ok(matchService.cancelMatch(matchId,authentication));
    }

    @PutMapping("/terminate/{matchId}")
    public ResponseEntity<MatchResponseDTO> terminate(
            @PathVariable Long matchId, Authentication authentication)
    {
        return ResponseEntity.ok(matchService.terminateMatch(matchId, authentication));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<MatchResponseDTO>> sent(Authentication authentication) {
        return ResponseEntity.ok(matchService.getMySentRequests(authentication));
    }

    @GetMapping("/received")
    public ResponseEntity<List<MatchResponseDTO>> received(Authentication authentication) {
        return ResponseEntity.ok(matchService.getMyReceivedRequests(authentication));
    }

    @GetMapping("/active")
    public ResponseEntity<List<MatchResponseDTO>> active(Authentication authentication) {
        return ResponseEntity.ok(matchService.getMyActiveMatches(authentication));
    }

    @GetMapping("/history")
    public ResponseEntity<List<MatchResponseDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(matchService.getMyMatchHistory(authentication));
    }

}